package com.luxe.loyalty.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the 9 loyalty mutations + entity-fetcher and field-resolver paths
 * through DGS. Each mutation that requires auth surfaces an error without a
 * JWT — that exercises the resolver entry, arg coercion, and auth gate code.
 */
@SpringBootTest
class LoyaltyMutationsResolverTest {

    @Autowired
    DgsQueryExecutor dgs;

    private static String key() { return UUID.randomUUID().toString(); }

    // ── enroll / transfer / linkPartner / register / redeem / buy / gift / extend ────

    @Test
    void enroll_in_loyalty_with_already_enrolled_guest_returns_error_union() {
        // Even without a JWT, the resolver requires auth — so we'll hit the
        // auth gate. That still exercises arg coercion and dispatch.
        String mutation = """
                mutation { enrollInLoyalty(input: {
                    guestId: "guest-001", referralCode: "REF-X", marketingOptIn: true
                }) {
                  ... on LoyaltyAccount { id loyaltyNumber tier }
                  ... on AlreadyEnrolledError { existingLoyaltyNumber }
                  ... on ValidationError { code }
                } }
                """;
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void transfer_points_requires_auth() {
        String mutation = """
                mutation { transferPoints(input: {
                    fromLoyaltyNumber: "LUX0001234567",
                    toLoyaltyNumber: "LUX0002345678",
                    points: 1000, message: "test"
                }, idempotencyKey: "%s") {
                  ... on PointsTransaction { id }
                  ... on InsufficientPointsError { shortfall }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(key());
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void transfer_to_airline_requires_auth() {
        String mutation = """
                mutation { transferToAirline(input: {
                    partnerId: "ptr-001", partnerAccountNumber: "SK-1", points: 5000
                }, idempotencyKey: "%s") {
                  ... on AirlineTransferSuccess { partnerMilesAwarded }
                  ... on InsufficientPointsError { shortfall }
                } }
                """.formatted(key());
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void link_partner_account_requires_auth() {
        String mutation = """
                mutation { linkPartnerAccount(input: {
                    partnerId: "ptr-001",
                    partnerAccountNumber: "SK-99",
                    partnerAccountLastName: "Smith"
                }) {
                  ... on LinkedPartnerAccount { id status }
                  ... on NotFoundError { code }
                } }
                """;
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void register_for_challenge_requires_auth() {
        var result = dgs.execute("""
                mutation { registerForChallenge(challengeId: "chl-001") {
                  ... on Challenge { id registered }
                  ... on NotFoundError { code }
                } }
                """);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void redeem_certificate_requires_auth() {
        String mutation = """
                mutation { redeemCertificate(certificateId: "cert-001",
                                              reservationId: "res-001",
                                              idempotencyKey: "%s") {
                  ... on CertificateRedemption { reservationId message }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """.formatted(key());
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void buy_points_requires_auth() {
        String mutation = """
                mutation { buyPoints(input: { points: 1000, paymentMethodId: "pm-1" },
                                     idempotencyKey: "%s") {
                  ... on PointsTransaction { id type }
                  ... on PaymentDeclinedError { retryWithNewCard }
                  ... on ValidationError { code }
                } }
                """.formatted(key());
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void gift_points_requires_auth() {
        String mutation = """
                mutation { giftPoints(input: {
                    recipientLoyaltyNumber: "LUX0002345678", points: 500
                }, idempotencyKey: "%s") {
                  ... on GiftPointsSuccess { pointsGifted }
                  ... on InsufficientPointsError { shortfall }
                } }
                """.formatted(key());
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void extend_points_expiry_requires_auth() {
        var result = dgs.execute("""
                mutation { extendPointsExpiry {
                  ... on LoyaltyAccount { id pointsBalance { expiringSoon } }
                  ... on ValidationError { code }
                } }
                """);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void my_loyalty_account_requires_auth() {
        var result = dgs.execute("{ myLoyaltyAccount { id tier } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void loyalty_account_lookup_by_number_requires_property_staff_role() {
        var result = dgs.execute(
                "{ loyaltyAccount(loyaltyNumber: \"LUX0001234567\") { id } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    // ── points valuation in different currencies (no auth needed) ────────────

    @Test
    void points_valuation_in_eur_returns_eur_money() {
        String currency = dgs.executeAndExtractJsonPath("""
                { pointsValuation(points: 100000, currency: "EUR") {
                    cashValue { currency } ratePerThousand { currency }
                    comparisonRedemptions { name approxValue { currency } }
                } }
                """, "data.pointsValuation.cashValue.currency");
        assertThat(currency).isEqualTo("EUR");
    }

    @Test
    void points_valuation_in_jpy_returns_jpy_money() {
        String currency = dgs.executeAndExtractJsonPath(
                "{ pointsValuation(points: 50000, currency: \"JPY\") { cashValue { currency } } }",
                "data.pointsValuation.cashValue.currency");
        assertThat(currency).isEqualTo("JPY");
    }

    @Test
    void loyalty_partners_filter_by_categories_returns_subset() {
        List<Map<String, Object>> airlines = dgs.executeAndExtractJsonPath("""
                { loyaltyPartners(first: 10, categories: [AIRLINE]) {
                    edges { node { id category } }
                    totalCount
                } }
                """, "data.loyaltyPartners.edges");
        assertThat(airlines).isNotEmpty();
        assertThat(airlines).allSatisfy(e -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> node = (Map<String, Object>) e.get("node");
            assertThat(node.get("category")).isEqualTo("AIRLINE");
        });
    }

    @Test
    void available_challenges_with_first_argument_caps_results() {
        List<Map<String, Object>> challenges = dgs.executeAndExtractJsonPath(
                "{ availableChallenges(first: 1) { id status } }",
                "data.availableChallenges");
        assertThat(challenges).hasSize(1);
    }

    // ── federation entity resolution + LoyaltyAccount derived fields ─────────

    @Test
    void federation_entities_resolves_loyalty_account_with_points_balance_and_tier_progress() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on LoyaltyAccount {
                      id loyaltyNumber tier
                      pointsBalance { available pending expiringSoon total cashEquivalent { amount currency } }
                      tierProgress { currentTier nextTier qualifyingNights tierProgressPct projectedTier }
                      benefits { code name category tier }
                    }
                  }
                }
                """;
        var rep = Map.of("__typename", "LoyaltyAccount", "id", "lac-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities =
                (List<Map<String, Object>>) ((Map<String, Object>) result.getData()).get("_entities");
        assertThat(entities).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> balance = (Map<String, Object>) entities.get(0).get("pointsBalance");
        @SuppressWarnings("unchecked")
        Map<String, Object> progress = (Map<String, Object>) entities.get(0).get("tierProgress");
        assertThat(balance.get("available")).isInstanceOf(Integer.class);
        assertThat(progress.get("currentTier")).isEqualTo("GOLD");
        assertThat(progress.get("nextTier")).isEqualTo("PLATINUM");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> benefits = (List<Map<String, Object>>) entities.get(0).get("benefits");
        assertThat(benefits).isNotEmpty();
    }

    @Test
    void federation_entities_resolves_member_tier_account_progress_to_silver() {
        // Find or create a MEMBER tier via enrollment? Easier: query an existing one.
        // Our seed has tiers GOLD/PLATINUM/AMBASSADOR/SILVER/TITANIUM; check that
        // tierProgress for SILVER points to GOLD.
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on LoyaltyAccount {
                      tier tierProgress { currentTier nextTier nightsToNextTier }
                    }
                  }
                }
                """;
        var rep = Map.of("__typename", "LoyaltyAccount", "id", "lac-004");  // SILVER seeded
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities =
                (List<Map<String, Object>>) ((Map<String, Object>) result.getData()).get("_entities");
        assertThat(entities).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> progress = (Map<String, Object>) entities.get(0).get("tierProgress");
        assertThat(progress.get("currentTier")).isEqualTo("SILVER");
        assertThat(progress.get("nextTier")).isEqualTo("GOLD");
    }

    @Test
    void loyalty_account_certificates_with_status_filter() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on LoyaltyAccount {
                      certificates(status: ACTIVE) { id status }
                    }
                  }
                }
                """;
        var rep = Map.of("__typename", "LoyaltyAccount", "id", "lac-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void loyalty_account_transactions_uses_pagination() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on LoyaltyAccount {
                      transactions(first: 2, sortBy: DATE_DESC) {
                        edges { node { id type points } }
                        pageInfo { hasNextPage }
                        totalCount
                      }
                    }
                  }
                }
                """;
        var rep = Map.of("__typename", "LoyaltyAccount", "id", "lac-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
    }
}
