package com.luxe.loyalty.datasource;

import com.luxe.common.scalar.Money;
import com.luxe.loyalty.schema.types.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class LoyaltyMockDataSource implements LoyaltyDataSource {

    private final Map<String, LoyaltyAccount> byId       = new LinkedHashMap<>();
    private final Map<String, LoyaltyAccount> byGuestId  = new LinkedHashMap<>();
    private final Map<String, LoyaltyAccount> byNumber   = new LinkedHashMap<>();
    private final List<PointsTransaction>     transactions = new ArrayList<>();
    private final Map<String, LoyaltyPartner> partners    = new LinkedHashMap<>();
    private final Map<String, Challenge>      challenges  = new LinkedHashMap<>();

    public LoyaltyMockDataSource() {
        initPartners();
        initChallenges();
        initAccounts();
        initTransactions();
        initCertificates();
        initLinkedPartners();
    }

    // ── Seed: Partners ───────────────────────────────────────────────────────

    private void initPartners() {
        addPartner("ptr-001", "SkyLuxe Airlines",         "AIRLINE",
                "Transfer points to SkyLuxe miles at a 2:1 ratio",
                0.5, 1000, 500000, 1000, true);
        addPartner("ptr-002", "Pacific Wings",            "AIRLINE",
                "Convert points to Pacific Wings miles",
                0.4, 2000, 200000, 1000, true);
        addPartner("ptr-003", "Stellar Drive",            "CAR_RENTAL",
                "Earn elite status and free upgrades with Stellar Drive",
                1.0, 500, null,    500, true);
        addPartner("ptr-004", "Coastal Cruises",          "CRUISE",
                "Apply points toward cabin upgrades on Coastal Cruises",
                0.6, 5000, 100000, 1000, true);
        addPartner("ptr-005", "Maison Gourmet",           "DINING",
                "Convert points to Maison Gourmet dining credits",
                1.2, 1000, 50000,  500, true);
        addPartner("ptr-006", "Atlas Card Rewards",       "FINANCIAL",
                "Link your Atlas Premium card for double points on every stay",
                1.0, 0,    null,   1, true);
    }

    private void addPartner(String id, String name, String cat, String desc,
                            double ratio, int min, Integer max, int inc, boolean active) {
        partners.put(id, new LoyaltyPartner(id, name, cat, desc, null, ratio, min, max, inc, active));
    }

    // ── Seed: Challenges ─────────────────────────────────────────────────────

    private void initChallenges() {
        LocalDate today = LocalDate.now();
        challenges.put("chl-001", new Challenge("chl-001",
                "Triple Continent Tour",
                "Stay at Luxe properties on three different continents in 90 days",
                "ACTIVE", today.minusDays(15), today.plusDays(75),
                new Challenge.ChallengeReward(15000, 0, null,
                        "Earn 15,000 bonus points after qualifying"),
                null, false));
        challenges.put("chl-002", new Challenge("chl-002",
                "Four Stays, One Suite",
                "Complete 4 paid stays for a complimentary Suite Night certificate",
                "ACTIVE", today.minusDays(30), today.plusDays(60),
                new Challenge.ChallengeReward(0, 1, "SUITE_NIGHT",
                        "One Suite Night certificate"),
                null, false));
        challenges.put("chl-003", new Challenge("chl-003",
                "Spring Break Bonus",
                "Earn 2x points on stays between March 1 and April 30",
                "ACTIVE", today.minusDays(10), today.plusDays(50),
                new Challenge.ChallengeReward(null, null, null,
                        "2x points on qualifying stays"),
                null, true));
        challenges.put("chl-004", new Challenge("chl-004",
                "Five-Star Foodie",
                "Dine at three Luxe restaurants during one stay",
                "UPCOMING", today.plusDays(20), today.plusDays(80),
                new Challenge.ChallengeReward(5000, 0, "DINING_CREDIT",
                        "5,000 bonus points + dining credit certificate"),
                null, false));
    }

    // ── Seed: Accounts ───────────────────────────────────────────────────────

    private void initAccounts() {
        addAccount("lac-001", "LUX0001234567", "guest-001", "GOLD", "ACTIVE",
                87500, 2400, 5000, 210000, 95,
                LocalDate.of(2021, 3, 1), "REF-AVA-2021",
                28, 38400, "USD");
        addAccount("lac-002", "LUX0002345678", "guest-002", "PLATINUM", "ACTIVE",
                42300, 1800, 0, 98000, 52,
                LocalDate.of(2019, 6, 15), "REF-LIAM-2019",
                52, 71250, "USD");
        addAccount("lac-003", "LUX0003456789", "guest-004", "AMBASSADOR", "ACTIVE",
                215000, 0, 12000, 580000, 175,
                LocalDate.of(2017, 1, 10), "REF-NORA-2017",
                118, 312000, "USD");
        addAccount("lac-004", "LUX0004567890", "guest-005", "SILVER", "ACTIVE",
                18700, 600, 0, 45000, 28,
                LocalDate.of(2022, 9, 20), "REF-SOFIA-2022",
                14, 9800, "USD");
        addAccount("lac-005", "LUX0005678901", "guest-007", "TITANIUM", "ACTIVE",
                132000, 4200, 8500, 305000, 130,
                LocalDate.of(2018, 11, 5), "REF-ETHAN-2018",
                72, 188500, "USD");
    }

    private void addAccount(String id, String number, String guestId, String tier, String status,
                            int avail, int pending, int expSoon, int lifetimePts, int lifetimeNights,
                            LocalDate since, String referralCode,
                            int qualNights, double qualSpend, String currency) {
        LoyaltyAccount a = new LoyaltyAccount(id, number, guestId, tier, status,
                avail, pending, expSoon, lifetimePts, lifetimeNights,
                since, referralCode, qualNights, qualSpend, currency);
        byId.put(id, a);
        byGuestId.put(guestId, a);
        byNumber.put(number, a);
    }

    // ── Seed: Transactions ───────────────────────────────────────────────────

    private void initTransactions() {
        OffsetDateTime now = OffsetDateTime.now();
        addTx("tx-001", "lac-001", "EARN_STAY",   5200,  5200,  "Stay at Le Grand Luxe Paris (res-101)", "res-101", null, now.minusDays(90));
        addTx("tx-002", "lac-001", "EARN_BONUS",  1000,  6200,  "Welcome bonus",                        null,      null, now.minusDays(89));
        addTx("tx-003", "lac-001", "EARN_STAY",   7500, 13700,  "Stay at Atlantis The Palm Dubai",      "res-102", null, now.minusDays(60));
        addTx("tx-004", "lac-001", "REDEEM",     -8000,  5700,  "Redeemed: Room category upgrade",      null,      null, now.minusDays(45));
        addTx("tx-005", "lac-001", "EARN_STAY",  12000, 17700,  "Stay at The Manhattan Pinnacle",       "res-103", null, now.minusDays(30));

        addTx("tx-101", "lac-002", "EARN_STAY",   4800,  4800,  "Stay at Mayfair Grand London",         "res-201", null, now.minusDays(120));
        addTx("tx-102", "lac-002", "EARN_STAY",   6300, 11100,  "Stay at Sakura Imperial Tokyo",        "res-202", null, now.minusDays(75));
        addTx("tx-103", "lac-002", "EARN_TRANSFER", 5000, 16100,"Transfer in from spouse account",      null,      null, now.minusDays(60));
        addTx("tx-104", "lac-002", "REDEEM",     -5000, 11100,  "Redeemed: $100 dining credit",         null,      null, now.minusDays(50));

        addTx("tx-201", "lac-003", "EARN_STAY",  22000, 22000,  "Stay at Atlantis Royal Villa Dubai",   "res-301", null, now.minusDays(200));
        addTx("tx-202", "lac-003", "EARN_BONUS", 25000, 47000,  "Ambassador status bonus",              null,      null, now.minusDays(199));
        addTx("tx-203", "lac-003", "REDEEM",   -25000, 22000,   "Redeemed: Free night certificate",     null,      null, now.minusDays(100));
        addTx("tx-204", "lac-003", "EARN_STAY", 18500, 40500,   "Stay at Le Grand Luxe Paris (Pres Suite)", "res-302", null, now.minusDays(30));
        addTx("tx-205", "lac-003", "EARN_PARTNER", 3500, 44000, "Partner: Stellar Drive elite week",    null,      "ptr-003", now.minusDays(20));

        addTx("tx-301", "lac-005", "EARN_STAY",  9800,  9800,   "Stay at The Manhattan Pinnacle",        "res-401", null, now.minusDays(150));
        addTx("tx-302", "lac-005", "EARN_STAY", 11200, 21000,   "Stay at Mayfair Grand London",          "res-402", null, now.minusDays(100));
        addTx("tx-303", "lac-005", "REDEEM",  -10000, 11000,    "Redeemed: Late checkout certificate",  null,      null, now.minusDays(80));
    }

    private void addTx(String id, String accountId, String type, int points, int after,
                       String desc, String resId, String partnerId, OffsetDateTime when) {
        OffsetDateTime expires = type.startsWith("EARN") ? when.plusYears(2) : null;
        transactions.add(new PointsTransaction(id, accountId, type, points, after,
                desc, resId, partnerId, when, expires));
    }

    // ── Seed: Certificates ───────────────────────────────────────────────────

    private void initCertificates() {
        OffsetDateTime now = OffsetDateTime.now();
        byId.get("lac-001").addCertificate(new Certificate(
                "cert-001", "FREE_NIGHT", "Free Night Award (35,000 pts)",
                "One complimentary night at participating Luxe properties",
                "ACTIVE", now.minusDays(40), now.plusDays(320),
                null, null, List.of(), List.of("Up to category 5 properties", "Standard rooms only")));

        byId.get("lac-002").addCertificate(new Certificate(
                "cert-002", "SUITE_NIGHT", "Suite Night Certificate",
                "Upgrade to a suite on a paid night, subject to availability",
                "ACTIVE", now.minusDays(60), now.plusDays(305),
                null, null, List.of(), List.of("Subject to suite availability at check-in")));

        byId.get("lac-003").addCertificate(new Certificate(
                "cert-003", "FREE_NIGHT", "Free Night Award (60,000 pts)",
                "One complimentary night at any Luxe property",
                "ACTIVE", now.minusDays(15), now.plusDays(350),
                null, null, List.of(), List.of("Subject to standard cancellation policy")));
        byId.get("lac-003").addCertificate(new Certificate(
                "cert-004", "DINING_CREDIT", "Ambassador Dining Credit ($500)",
                "Annual dining credit, valid at signature restaurants",
                "REDEEMED", now.minusDays(120), now.plusDays(245),
                now.minusDays(40), "res-302", List.of(), List.of()));

        byId.get("lac-005").addCertificate(new Certificate(
                "cert-005", "BREAKFAST", "Daily Breakfast Certificate",
                "Complimentary daily breakfast for two",
                "ACTIVE", now.minusDays(10), now.plusDays(355),
                null, null, List.of(), List.of()));
    }

    // ── Seed: Linked partners ────────────────────────────────────────────────

    private void initLinkedPartners() {
        OffsetDateTime now = OffsetDateTime.now();
        byId.get("lac-002").addLinkedPartner(new LinkedPartnerAccount(
                "lpa-001", partners.get("ptr-001"), "SK-83472918",
                now.minusDays(180), now.minusDays(15), "ACTIVE"));
        byId.get("lac-003").addLinkedPartner(new LinkedPartnerAccount(
                "lpa-002", partners.get("ptr-001"), "SK-91002384",
                now.minusDays(400), now.minusDays(3), "ACTIVE"));
        byId.get("lac-003").addLinkedPartner(new LinkedPartnerAccount(
                "lpa-003", partners.get("ptr-006"), "ATLAS-7821-1029",
                now.minusDays(220), now.minusDays(1), "ACTIVE"));
        byId.get("lac-005").addLinkedPartner(new LinkedPartnerAccount(
                "lpa-004", partners.get("ptr-003"), "STELLAR-44918",
                now.minusDays(60), now.minusDays(7), "ACTIVE"));
    }

    // ── Account lookups ──────────────────────────────────────────────────────

    @Override public Optional<LoyaltyAccount> findByGuestId(String guestId)         { return Optional.ofNullable(byGuestId.get(guestId)); }
    @Override public Optional<LoyaltyAccount> findByLoyaltyNumber(String number)    { return Optional.ofNullable(byNumber.get(number)); }
    @Override public Optional<LoyaltyAccount> findById(String id)                   { return Optional.ofNullable(byId.get(id)); }

    @Override
    public LoyaltyAccount enroll(String guestId, String referralCode, Boolean marketingOptIn) {
        String number = "LUX" + String.format("%010d", new Random().nextInt(999_999_999));
        String id = "lac-" + UUID.randomUUID().toString().substring(0, 8);
        String ref = "REF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LoyaltyAccount a = new LoyaltyAccount(id, number, guestId, "MEMBER", "ACTIVE",
                0, 0, 0, 0, 0, LocalDate.now(), ref, 0, 0.0, "USD");
        byId.put(id, a);
        byGuestId.put(guestId, a);
        byNumber.put(number, a);
        return a;
    }

    // ── Transactions ─────────────────────────────────────────────────────────

    @Override
    public List<PointsTransaction> findTransactions(String accountId, Map<String, Object> filter, String sortBy) {
        String type      = filter != null ? (String) filter.get("type") : null;
        String fromDate  = filter != null ? (String) filter.get("fromDate") : null;
        String toDate    = filter != null ? (String) filter.get("toDate")   : null;
        String resId     = filter != null ? (String) filter.get("reservationId") : null;
        return transactions.stream()
                .filter(t -> t.accountId().equals(accountId))
                .filter(t -> type == null || t.type().equals(type))
                .filter(t -> resId == null || resId.equals(t.reservationId()))
                .filter(t -> fromDate == null || !t.transactionDate().toLocalDate().isBefore(LocalDate.parse(fromDate)))
                .filter(t -> toDate   == null || !t.transactionDate().toLocalDate().isAfter(LocalDate.parse(toDate)))
                .sorted(transactionComparator(sortBy))
                .collect(Collectors.toList());
    }

    private Comparator<PointsTransaction> transactionComparator(String sortBy) {
        if (sortBy == null) sortBy = "DATE_DESC";
        return switch (sortBy) {
            case "DATE_ASC"     -> Comparator.comparing(PointsTransaction::transactionDate);
            case "POINTS_DESC"  -> Comparator.comparingInt(PointsTransaction::points).reversed();
            case "POINTS_ASC"   -> Comparator.comparingInt(PointsTransaction::points);
            default             -> Comparator.comparing(PointsTransaction::transactionDate).reversed();
        };
    }

    @Override
    public List<Certificate> findCertificates(String accountId, String status) {
        LoyaltyAccount a = byId.get(accountId);
        if (a == null) return List.of();
        return a.getCertificatesRaw().stream()
                .filter(c -> status == null || status.equals(c.getStatus()))
                .collect(Collectors.toList());
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    @Override
    public PointsTransaction transferPoints(String fromNumber, String toNumber, int points, String message) {
        LoyaltyAccount from = byNumber.get(fromNumber);
        LoyaltyAccount to   = byNumber.get(toNumber);
        if (from == null || to == null) return null;
        if (from.getPointsAvailable() < points) return null;
        from.deductAvailable(points);
        to.addAvailable(points);
        recordTx(from.getId(), "GIFT_OUT", -points, from.getPointsAvailable(),
                "Transfer to " + toNumber + (message != null ? " — " + message : ""), null, null);
        return recordTx(to.getId(), "GIFT_IN", points, to.getPointsAvailable(),
                "Transfer from " + fromNumber + (message != null ? " — " + message : ""), null, null);
    }

    @Override
    public PointsTransaction transferToAirline(String accountId, String partnerId,
                                                String partnerAccountNumber, int points) {
        LoyaltyAccount a = byId.get(accountId);
        LoyaltyPartner p = partners.get(partnerId);
        if (a == null || p == null) return null;
        if (a.getPointsAvailable() < points) return null;
        a.deductAvailable(points);
        return recordTx(a.getId(), "REDEEM_TRANSFER", -points, a.getPointsAvailable(),
                "Transfer to " + p.name() + " account " + partnerAccountNumber,
                null, partnerId);
    }

    @Override
    public LinkedPartnerAccount linkPartnerAccount(String accountId, String partnerId,
                                                    String partnerAccountNumber, String lastName) {
        LoyaltyAccount a = byId.get(accountId);
        LoyaltyPartner p = partners.get(partnerId);
        if (a == null || p == null) return null;
        LinkedPartnerAccount link = new LinkedPartnerAccount(
                "lpa-" + UUID.randomUUID().toString().substring(0, 8),
                p, partnerAccountNumber, OffsetDateTime.now(), OffsetDateTime.now(), "ACTIVE");
        a.addLinkedPartner(link);
        return link;
    }

    @Override
    public Challenge registerForChallenge(String accountId, String challengeId) {
        Challenge c = challenges.get(challengeId);
        if (c == null) return null;
        c.setRegistered(true);
        if (c.getProgress() == null) {
            c.setProgress(Challenge.ChallengeProgress.of(0, 4));
        }
        return c;
    }

    @Override
    public Certificate redeemCertificate(String accountId, String certificateId, String reservationId) {
        LoyaltyAccount a = byId.get(accountId);
        if (a == null) return null;
        Certificate cert = a.getCertificatesRaw().stream()
                .filter(c -> c.getId().equals(certificateId))
                .findFirst().orElse(null);
        if (cert == null) return null;
        if (!"ACTIVE".equals(cert.getStatus())) return null;
        cert.redeem(reservationId);
        recordTx(a.getId(), "REDEEM_CERTIFICATE", 0, a.getPointsAvailable(),
                "Redeemed certificate: " + cert.getName(), reservationId, null);
        return cert;
    }

    @Override
    public PointsTransaction buyPoints(String accountId, int points, String paymentMethodId) {
        LoyaltyAccount a = byId.get(accountId);
        if (a == null) return null;
        a.addAvailable(points);
        a.addLifetime(points);
        return recordTx(a.getId(), "EARN_PURCHASE", points, a.getPointsAvailable(),
                "Purchased " + points + " points", null, null);
    }

    @Override
    public PointsTransaction giftPoints(String fromNumber, String toNumber, int points, String message) {
        return transferPoints(fromNumber, toNumber, points, message);
    }

    @Override
    public LoyaltyAccount extendPointsExpiry(String accountId) {
        LoyaltyAccount a = byId.get(accountId);
        if (a == null) return null;
        a.extendExpiry();
        return a;
    }

    private PointsTransaction recordTx(String accountId, String type, int points, int balanceAfter,
                                        String desc, String resId, String partnerId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expires = type.startsWith("EARN") ? now.plusYears(2) : null;
        PointsTransaction tx = new PointsTransaction(
                "tx-" + UUID.randomUUID().toString().substring(0, 8),
                accountId, type, points, balanceAfter, desc, resId, partnerId, now, expires);
        transactions.add(tx);
        return tx;
    }

    // ── Partners / challenges / benefits / valuation ─────────────────────────

    @Override
    public List<LoyaltyPartner> findPartners(List<String> categories) {
        return partners.values().stream()
                .filter(p -> categories == null || categories.isEmpty()
                        || categories.contains(p.category()))
                .collect(Collectors.toList());
    }

    @Override public Optional<LoyaltyPartner> findPartnerById(String id) {
        return Optional.ofNullable(partners.get(id));
    }

    @Override
    public List<Challenge> findAvailableChallenges() {
        return challenges.values().stream()
                .filter(c -> "ACTIVE".equals(c.getStatus()) || "UPCOMING".equals(c.getStatus()))
                .collect(Collectors.toList());
    }

    @Override public Optional<Challenge> findChallengeById(String id) {
        return Optional.ofNullable(challenges.get(id));
    }

    @Override
    public List<LoyaltyBenefit> benefitsForTier(String tier) {
        return switch (tier) {
            case "MEMBER" -> List.of(
                    benefit("MEMBER_RATES",   "Member rates",            "Exclusive member-only nightly rates",         "POINTS",  "MEMBER"),
                    benefit("MEMBER_POINTS",  "Earn points",             "Earn 10 points per dollar on qualifying spend","POINTS", "MEMBER"));
            case "SILVER" -> List.of(
                    benefit("SILVER_LATE",    "Late check-out (1 PM)",   "Guaranteed late check-out subject to availability","CHECKOUT","SILVER"),
                    benefit("SILVER_UPG",     "Room upgrade",            "Complimentary upgrade when available",        "ROOM",    "SILVER"),
                    benefit("SILVER_BONUS",   "25% point bonus",         "Earn 25% bonus points on every stay",         "POINTS",  "SILVER"));
            case "GOLD" -> List.of(
                    benefit("GOLD_UPG",       "Confirmed upgrade",       "Confirmed upgrade up to junior suite",        "ROOM",    "GOLD"),
                    benefit("GOLD_LATE",      "Late check-out (2 PM)",   "Guaranteed late check-out",                   "CHECKOUT","GOLD"),
                    benefit("GOLD_BONUS",     "50% point bonus",         "Earn 50% bonus points on every stay",         "POINTS",  "GOLD"),
                    benefit("GOLD_INTERNET", "Premium WiFi",            "Complimentary premium internet",              "INTERNET","GOLD"));
            case "PLATINUM" -> List.of(
                    benefit("PLAT_SUITE",     "Suite upgrade",           "Suite upgrade when available",                "ROOM",    "PLATINUM"),
                    benefit("PLAT_BREAK",     "Daily breakfast",         "Complimentary breakfast for two",             "DINING",  "PLATINUM"),
                    benefit("PLAT_LOUNGE",    "Lounge access",           "Executive lounge access",                     "LOUNGE",  "PLATINUM"),
                    benefit("PLAT_BONUS",     "75% point bonus",         "Earn 75% bonus points on every stay",         "POINTS",  "PLATINUM"),
                    benefit("PLAT_LATE",      "Late check-out (4 PM)",   "Guaranteed late check-out",                   "CHECKOUT","PLATINUM"));
            case "TITANIUM" -> List.of(
                    benefit("TIT_SUITE",      "Confirmed suite upgrade", "Confirmed suite upgrade subject to inventory","ROOM",    "TITANIUM"),
                    benefit("TIT_AMB",        "Personal Ambassador",     "Dedicated Ambassador concierge",              "CONCIERGE","TITANIUM"),
                    benefit("TIT_BREAK",      "Daily breakfast",         "Complimentary breakfast for two",             "DINING",  "TITANIUM"),
                    benefit("TIT_LOUNGE",     "Lounge access",           "Premier lounge access for member + 1",        "LOUNGE",  "TITANIUM"),
                    benefit("TIT_BONUS",     "100% point bonus",        "Earn 100% bonus points on every stay",        "POINTS",  "TITANIUM"),
                    benefit("TIT_CERT",       "Annual free night",       "Annual free night certificate",               "CERTIFICATE","TITANIUM"));
            case "AMBASSADOR" -> List.of(
                    benefit("AMB_AMB",        "Ambassador team",         "Dedicated Ambassador team available 24/7",    "CONCIERGE","AMBASSADOR"),
                    benefit("AMB_SUITE",      "Guaranteed suite",        "Guaranteed suite at every stay",              "ROOM",    "AMBASSADOR"),
                    benefit("AMB_TRANSPORT", "Airport transfers",        "Complimentary private airport transfers",     "TRANSPORT","AMBASSADOR"),
                    benefit("AMB_LOUNGE",     "Diamond lounges",         "Access to exclusive Diamond lounges",         "LOUNGE",  "AMBASSADOR"),
                    benefit("AMB_BONUS",      "125% point bonus",        "Earn 125% bonus points on every stay",        "POINTS",  "AMBASSADOR"),
                    benefit("AMB_CERTS",      "Two free nights",         "Two annual free night certificates",          "CERTIFICATE","AMBASSADOR"));
            default -> List.of();
        };
    }

    private LoyaltyBenefit benefit(String code, String name, String desc, String cat, String tier) {
        return new LoyaltyBenefit(code, name, desc, cat, tier);
    }

    @Override
    public PointsValuation valuePoints(int points, String currency) {
        double cents = 0.7;
        double rate = cents / 100.0;
        Money cash = Money.of(points * rate, currency);
        Money per1k = Money.of(1000 * rate, currency);
        return new PointsValuation(points, currency, cash, per1k,
                "Free night certificates often deliver 2-3x base value vs cash",
                List.of(
                        new PointsValuation.PointsRedemptionExample(
                                "Free Night (Category 5)", 35000, Money.of(450, currency)),
                        new PointsValuation.PointsRedemptionExample(
                                "Suite Night Award",       60000, Money.of(900, currency)),
                        new PointsValuation.PointsRedemptionExample(
                                "Spa Treatment",           12000, Money.of(280, currency)),
                        new PointsValuation.PointsRedemptionExample(
                                "$100 Dining Credit",       5000, Money.of(100, currency))));
    }
}
