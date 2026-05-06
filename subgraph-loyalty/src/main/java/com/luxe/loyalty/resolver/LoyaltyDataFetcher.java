package com.luxe.loyalty.resolver;

import com.luxe.loyalty.schema.types.GuestProfile;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthContextResolver;
import com.luxe.common.auth.AuthRole;
import com.luxe.common.error.AlreadyEnrolledError;
import com.luxe.common.error.InsufficientPointsError;
import com.luxe.common.error.NotFoundError;
import com.luxe.common.error.ValidationError;
import com.luxe.common.error.FieldError;
import com.luxe.common.pagination.Connection;
import com.luxe.common.scalar.Money;
import com.luxe.loyalty.datasource.LoyaltyDataSource;
import com.luxe.loyalty.schema.types.*;
import com.netflix.graphql.dgs.*;
import graphql.schema.DataFetchingEnvironment;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@DgsComponent
public class LoyaltyDataFetcher {

    private final LoyaltyDataSource dataSource;
    private final AuthContextResolver authResolver;

    public LoyaltyDataFetcher(LoyaltyDataSource dataSource, AuthContextResolver authResolver) {
        this.dataSource = dataSource;
        this.authResolver = authResolver;
    }

    private AuthContext getAuth(DataFetchingEnvironment dfe) {
        return authResolver.resolve(dfe);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @DgsQuery
    public LoyaltyAccount myLoyaltyAccount(DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        return dataSource.findByGuestId(auth.guestId()).orElse(null);
    }

    @DgsQuery
    public LoyaltyAccount loyaltyAccount(@InputArgument String loyaltyNumber, DataFetchingEnvironment dfe) {
        getAuth(dfe).requireRole(AuthRole.PROPERTY_STAFF);
        return dataSource.findByLoyaltyNumber(loyaltyNumber).orElse(null);
    }

    @DgsQuery
    public Object loyaltyPartners(@InputArgument Integer first,
                                   @InputArgument List<String> categories) {
        List<LoyaltyPartner> all = dataSource.findPartners(categories);
        Connection<LoyaltyPartner> conn = Connection.of(all, first != null ? first : 20, null);
        return Map.of(
                "edges", conn.edges().stream()
                        .map(e -> Map.of("node", e.node(), "cursor", e.cursor())).toList(),
                "pageInfo", pageInfo(conn),
                "totalCount", conn.totalCount());
    }

    @DgsQuery
    public PointsValuation pointsValuation(@InputArgument int points,
                                            @InputArgument String currency) {
        return dataSource.valuePoints(points, currency);
    }

    @DgsQuery
    public List<Challenge> availableChallenges(@InputArgument Integer first) {
        List<Challenge> all = dataSource.findAvailableChallenges();
        return first != null ? all.stream().limit(first).toList() : all;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @DgsMutation
    public Object enrollInLoyalty(@InputArgument Map<String, Object> input,
                                    DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        String guestId = (String) input.get("guestId");
        if (guestId == null) {
            return new ValidationError("MISSING_GUEST_ID", "guestId is required",
                    List.of(new FieldError("guestId", "Required")));
        }
        return dataSource.findByGuestId(guestId)
                .<Object>map(existing -> new AlreadyEnrolledError(existing.getLoyaltyNumber()))
                .orElseGet(() -> dataSource.enroll(guestId,
                        (String) input.get("referralCode"),
                        (Boolean) input.get("marketingOptIn")));
    }

    @DgsMutation
    public Object transferPoints(@InputArgument Map<String, Object> input,
                                  @InputArgument String idempotencyKey,
                                  DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        String fromNum = (String) input.get("fromLoyaltyNumber");
        String toNum   = (String) input.get("toLoyaltyNumber");
        Integer points = input.get("points") != null ? ((Number) input.get("points")).intValue() : null;
        if (fromNum == null || toNum == null || points == null || points <= 0) {
            return new ValidationError("INVALID_INPUT",
                    "fromLoyaltyNumber, toLoyaltyNumber, and a positive points value are required",
                    List.of());
        }
        var from = dataSource.findByLoyaltyNumber(fromNum).orElse(null);
        var to   = dataSource.findByLoyaltyNumber(toNum).orElse(null);
        if (from == null) return new NotFoundError("LoyaltyAccount", fromNum);
        if (to   == null) return new NotFoundError("LoyaltyAccount", toNum);
        if (from.getPointsAvailable() < points) {
            return new InsufficientPointsError(from.getPointsAvailable(), points);
        }
        return dataSource.transferPoints(fromNum, toNum, points, (String) input.get("message"));
    }

    @DgsMutation
    public Object transferToAirline(@InputArgument Map<String, Object> input,
                                     @InputArgument String idempotencyKey,
                                     DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        String partnerId = (String) input.get("partnerId");
        String partnerAcct = (String) input.get("partnerAccountNumber");
        Integer points = input.get("points") != null ? ((Number) input.get("points")).intValue() : null;
        if (partnerId == null || partnerAcct == null || points == null || points <= 0) {
            return new ValidationError("INVALID_INPUT",
                    "partnerId, partnerAccountNumber, and positive points are required",
                    List.of());
        }
        var partner = dataSource.findPartnerById(partnerId).orElse(null);
        if (partner == null) return new NotFoundError("LoyaltyPartner", partnerId);
        var account = dataSource.findByGuestId(auth.guestId()).orElse(null);
        if (account == null) return new NotFoundError("LoyaltyAccount", auth.guestId());
        if (account.getPointsAvailable() < points) {
            return new InsufficientPointsError(account.getPointsAvailable(), points);
        }
        var tx = dataSource.transferToAirline(account.getId(), partnerId, partnerAcct, points);
        if (tx == null) return new ValidationError("TRANSFER_FAILED", "Transfer could not be processed", List.of());
        int milesAwarded = (int) Math.round(points * partner.pointsRatio());
        return new AirlineTransferSuccess(tx.id(), points, milesAwarded, partner, 3,
                "Transfer initiated. Miles will be credited to " + partnerAcct
                        + " within 3 business days.");
    }

    @DgsMutation
    public Object linkPartnerAccount(@InputArgument Map<String, Object> input,
                                      DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        String partnerId = (String) input.get("partnerId");
        String partnerAcct = (String) input.get("partnerAccountNumber");
        if (partnerId == null || partnerAcct == null) {
            return new ValidationError("INVALID_INPUT",
                    "partnerId and partnerAccountNumber are required", List.of());
        }
        if (dataSource.findPartnerById(partnerId).isEmpty())
            return new NotFoundError("LoyaltyPartner", partnerId);
        var account = dataSource.findByGuestId(auth.guestId()).orElse(null);
        if (account == null) return new NotFoundError("LoyaltyAccount", auth.guestId());
        return dataSource.linkPartnerAccount(account.getId(), partnerId, partnerAcct,
                (String) input.get("partnerAccountLastName"));
    }

    @DgsMutation
    public Object registerForChallenge(@InputArgument String challengeId,
                                        DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        var account = dataSource.findByGuestId(auth.guestId()).orElse(null);
        if (account == null) return new NotFoundError("LoyaltyAccount", auth.guestId());
        var challenge = dataSource.registerForChallenge(account.getId(), challengeId);
        return challenge != null ? challenge : new NotFoundError("Challenge", challengeId);
    }

    @DgsMutation
    public Object redeemCertificate(@InputArgument String certificateId,
                                     @InputArgument String reservationId,
                                     @InputArgument String idempotencyKey,
                                     DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        var account = dataSource.findByGuestId(auth.guestId()).orElse(null);
        if (account == null) return new NotFoundError("LoyaltyAccount", auth.guestId());
        var cert = dataSource.redeemCertificate(account.getId(), certificateId, reservationId);
        if (cert == null) {
            return new ValidationError("INVALID_CERTIFICATE",
                    "Certificate not found, already redeemed, or expired", List.of());
        }
        return new CertificateRedemption(cert, reservationId,
                OffsetDateTime.now(),
                "Certificate applied to reservation " + reservationId);
    }

    @DgsMutation
    public Object buyPoints(@InputArgument Map<String, Object> input,
                              @InputArgument String idempotencyKey,
                              DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        Integer points = input.get("points") != null ? ((Number) input.get("points")).intValue() : null;
        if (points == null || points <= 0) {
            return new ValidationError("INVALID_INPUT",
                    "Positive points value is required", List.of());
        }
        var account = dataSource.findByGuestId(auth.guestId()).orElse(null);
        if (account == null) return new ValidationError("NO_ACCOUNT",
                "No loyalty account on file for the authenticated user", List.of());
        return dataSource.buyPoints(account.getId(), points, (String) input.get("paymentMethodId"));
    }

    @DgsMutation
    public Object giftPoints(@InputArgument Map<String, Object> input,
                              @InputArgument String idempotencyKey,
                              DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        Integer points = input.get("points") != null ? ((Number) input.get("points")).intValue() : null;
        String recipient = (String) input.get("recipientLoyaltyNumber");
        if (points == null || points <= 0 || recipient == null) {
            return new ValidationError("INVALID_INPUT",
                    "recipientLoyaltyNumber and positive points are required", List.of());
        }
        var sender = dataSource.findByGuestId(auth.guestId()).orElse(null);
        if (sender == null) return new NotFoundError("LoyaltyAccount", auth.guestId());
        var recipientAcct = dataSource.findByLoyaltyNumber(recipient).orElse(null);
        if (recipientAcct == null) return new NotFoundError("LoyaltyAccount", recipient);
        if (sender.getPointsAvailable() < points)
            return new InsufficientPointsError(sender.getPointsAvailable(), points);
        var tx = dataSource.giftPoints(sender.getLoyaltyNumber(), recipient, points,
                (String) input.get("message"));
        return new GiftPointsSuccess(tx.id(), recipient, points,
                sender.getPointsAvailable(),
                "Gifted " + points + " points to " + recipient);
    }

    @DgsMutation
    public Object extendPointsExpiry(DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        var account = dataSource.findByGuestId(auth.guestId()).orElse(null);
        if (account == null) return new ValidationError("NO_ACCOUNT",
                "No loyalty account on file", List.of());
        return dataSource.extendPointsExpiry(account.getId());
    }

    // ── LoyaltyAccount derived field resolvers ────────────────────────────────

    @DgsData(parentType = "LoyaltyAccount", field = "pointsBalance")
    public PointsBalance pointsBalance(DataFetchingEnvironment dfe) {
        LoyaltyAccount a = dfe.getSource();
        int total = a.getPointsAvailable() + a.getPointsPending();
        Money cash = Money.of(a.getPointsAvailable() * 0.007, a.getCurrency());
        return new PointsBalance(a.getPointsAvailable(), a.getPointsPending(),
                a.getPointsExpiringSoon(), total, cash, a.getUpdatedAt());
    }

    @DgsData(parentType = "LoyaltyAccount", field = "tierProgress")
    public TierProgress tierProgress(DataFetchingEnvironment dfe) {
        LoyaltyAccount a = dfe.getSource();
        return computeTierProgress(a);
    }

    @DgsData(parentType = "LoyaltyAccount", field = "benefits")
    public List<LoyaltyBenefit> benefits(DataFetchingEnvironment dfe) {
        LoyaltyAccount a = dfe.getSource();
        return dataSource.benefitsForTier(a.getTier());
    }

    @DgsData(parentType = "LoyaltyAccount", field = "certificates")
    public List<Certificate> certificates(DataFetchingEnvironment dfe) {
        LoyaltyAccount a = dfe.getSource();
        Integer first = dfe.getArgument("first");
        String status = dfe.getArgument("status");
        List<Certificate> certs = dataSource.findCertificates(a.getId(), status);
        return first != null ? certs.stream().limit(first).toList() : certs;
    }

    @DgsData(parentType = "LoyaltyAccount", field = "transactions")
    public Object transactions(DataFetchingEnvironment dfe) {
        LoyaltyAccount a = dfe.getSource();
        Integer first = dfe.getArgument("first");
        String after  = dfe.getArgument("after");
        Map<String, Object> filter = dfe.getArgument("filter");
        String sortBy = dfe.getArgument("sortBy");
        List<PointsTransaction> all = dataSource.findTransactions(a.getId(), filter, sortBy);
        Connection<PointsTransaction> conn = Connection.of(all, first != null ? first : 20, after);
        return Map.of(
                "edges", conn.edges().stream()
                        .map(e -> Map.of("node", e.node(), "cursor", e.cursor())).toList(),
                "pageInfo", pageInfo(conn),
                "totalCount", conn.totalCount());
    }

    @DgsData(parentType = "LoyaltyAccount", field = "availableChallenges")
    public List<Challenge> accountAvailableChallenges(DataFetchingEnvironment dfe) {
        return dataSource.findAvailableChallenges();
    }

    // ── GuestProfile.loyaltyAccount (federated) ───────────────────────────────

    @DgsData(parentType = "GuestProfile", field = "loyaltyAccount")
    public LoyaltyAccount guestLoyaltyAccount(DataFetchingEnvironment dfe) {
        GuestProfile guest = dfe.getSource();
        return dataSource.findByGuestId(guest.getId()).orElse(null);
    }

    // ── Entity fetcher ────────────────────────────────────────────────────────

    @DgsEntityFetcher(name = "LoyaltyAccount")
    public LoyaltyAccount fetchAccount(Map<String, Object> values) {
        return dataSource.findById((String) values.get("id")).orElse(null);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private TierProgress computeTierProgress(LoyaltyAccount a) {
        int qualifying = a.getQualifyingNights();
        String currentTier = a.getTier();
        String nextTier = nextTier(currentTier);
        Integer toNext = nextTier == null ? null
                : Math.max(0, tierThreshold(nextTier) - qualifying);
        Integer toRetain = currentTier.equals("MEMBER") ? null
                : Math.max(0, tierThreshold(currentTier) - qualifying);
        double pct = nextTier == null ? 100.0
                : Math.min(100.0, (qualifying * 100.0) / tierThreshold(nextTier));
        Money spend = Money.of(a.getQualifyingSpend(), a.getCurrency());
        return new TierProgress(currentTier, nextTier, qualifying, toNext, toRetain,
                spend, pct, LocalDate.of(LocalDate.now().getYear(), 12, 31),
                projectTier(qualifying));
    }

    private String nextTier(String tier) {
        return switch (tier) {
            case "MEMBER"     -> "SILVER";
            case "SILVER"     -> "GOLD";
            case "GOLD"       -> "PLATINUM";
            case "PLATINUM"   -> "TITANIUM";
            case "TITANIUM"   -> "AMBASSADOR";
            default           -> null;
        };
    }

    private int tierThreshold(String tier) {
        return switch (tier) {
            case "MEMBER"     -> 0;
            case "SILVER"     -> 10;
            case "GOLD"       -> 25;
            case "PLATINUM"   -> 50;
            case "TITANIUM"   -> 75;
            case "AMBASSADOR" -> 100;
            default           -> 0;
        };
    }

    private String projectTier(int qualifying) {
        if (qualifying >= 100) return "AMBASSADOR";
        if (qualifying >= 75)  return "TITANIUM";
        if (qualifying >= 50)  return "PLATINUM";
        if (qualifying >= 25)  return "GOLD";
        if (qualifying >= 10)  return "SILVER";
        return "MEMBER";
    }

    private Map<String, Object> pageInfo(Connection<?> conn) {
        java.util.HashMap<String, Object> info = new java.util.HashMap<>();
        info.put("hasNextPage", conn.pageInfo().hasNextPage());
        info.put("hasPreviousPage", conn.pageInfo().hasPreviousPage());
        info.put("startCursor", conn.pageInfo().startCursor());
        info.put("endCursor", conn.pageInfo().endCursor());
        return info;
    }
    @DgsEntityFetcher(name = "GuestProfile")
    public GuestProfile fetchGuestProfileReference(Map<String, Object> values) {
        return new GuestProfile((String) values.get("id"));
    }

}
