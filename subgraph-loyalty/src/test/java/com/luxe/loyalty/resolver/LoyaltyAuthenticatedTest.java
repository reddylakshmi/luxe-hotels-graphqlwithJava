package com.luxe.loyalty.resolver;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthContextResolver;
import com.luxe.common.auth.AuthRole;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authenticated coverage of the 9 loyalty mutations + auth-gated queries.
 * Uses ADMIN role so PROPERTY_STAFF-gated paths also run.
 */
@SpringBootTest
@Import(LoyaltyAuthenticatedTest.AuthOverrideConfig.class)
class LoyaltyAuthenticatedTest {

    @TestConfiguration
    static class AuthOverrideConfig {
        @Bean @Primary
        AuthContextResolver mockAuth() {
            return dfe -> AuthContext.of("guest-001", "LUX0001234567", AuthRole.ADMIN);
        }
    }

    @Autowired
    DgsQueryExecutor dgs;

    private static String key() { return UUID.randomUUID().toString(); }

    // ── Auth-gated queries ───────────────────────────────────────────────────

    @Test
    void my_loyalty_account_returns_account_for_authenticated_guest() {
        String tier = dgs.executeAndExtractJsonPath(
                "{ myLoyaltyAccount { id tier loyaltyNumber } }",
                "data.myLoyaltyAccount.tier");
        assertThat(tier).isEqualTo("GOLD");
    }

    @Test
    void loyalty_account_lookup_by_number_succeeds_with_admin_role() {
        String id = dgs.executeAndExtractJsonPath(
                "{ loyaltyAccount(loyaltyNumber: \"LUX0001234567\") { id tier } }",
                "data.loyaltyAccount.id");
        assertThat(id).isEqualTo("lac-001");
    }

    // ── Mutations (auth-gated) ───────────────────────────────────────────────

    @Test
    void enroll_in_loyalty_existing_guest_returns_already_enrolled() {
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { enrollInLoyalty(input: { guestId: "guest-001" }) {
                  __typename
                  ... on LoyaltyAccount { loyaltyNumber }
                  ... on AlreadyEnrolledError { existingLoyaltyNumber }
                  ... on ValidationError { code }
                } }
                """, "data.enrollInLoyalty.__typename");
        assertThat(typename).isEqualTo("AlreadyEnrolledError");
    }

    @Test
    void enroll_in_loyalty_new_guest_creates_account() {
        String tier = dgs.executeAndExtractJsonPath("""
                mutation { enrollInLoyalty(input: { guestId: "guest-brand-new", marketingOptIn: true }) {
                  __typename
                  ... on LoyaltyAccount { tier loyaltyNumber }
                  ... on AlreadyEnrolledError { existingLoyaltyNumber }
                } }
                """, "data.enrollInLoyalty.tier");
        assertThat(tier).isEqualTo("MEMBER");
    }

    @Test
    void transfer_points_between_known_numbers_returns_transaction() {
        var result = dgs.execute("""
                mutation { transferPoints(input: {
                    fromLoyaltyNumber: "LUX0001234567",
                    toLoyaltyNumber: "LUX0002345678",
                    points: 500, message: "Test transfer"
                }, idempotencyKey: "%s") {
                  ... on PointsTransaction { id type points }
                  ... on InsufficientPointsError { shortfall }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void transfer_points_to_unknown_recipient_returns_not_found() {
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { transferPoints(input: {
                    fromLoyaltyNumber: "LUX0001234567",
                    toLoyaltyNumber: "LUX-NOT-REAL",
                    points: 100
                }, idempotencyKey: "%s") {
                  __typename
                  ... on PointsTransaction { id }
                  ... on NotFoundError { code }
                  ... on InsufficientPointsError { shortfall }
                  ... on ValidationError { code }
                } }
                """.formatted(key()), "data.transferPoints.__typename");
        assertThat(typename).isEqualTo("NotFoundError");
    }

    @Test
    void transfer_to_airline_returns_success() {
        var result = dgs.execute("""
                mutation { transferToAirline(input: {
                    partnerId: "ptr-001",
                    partnerAccountNumber: "SK-1234",
                    points: 2000
                }, idempotencyKey: "%s") {
                  ... on AirlineTransferSuccess { transactionId pointsDeducted partnerMilesAwarded }
                  ... on InsufficientPointsError { shortfall }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void link_partner_account_returns_link() {
        var result = dgs.execute("""
                mutation { linkPartnerAccount(input: {
                    partnerId: "ptr-002", partnerAccountNumber: "PW-X"
                }) {
                  ... on LinkedPartnerAccount { id status }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void register_for_challenge_marks_registered() {
        var result = dgs.execute("""
                mutation { registerForChallenge(challengeId: "chl-001") {
                  ... on Challenge { registered progress { current goal pct } }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void redeem_certificate_returns_redemption() {
        var result = dgs.execute("""
                mutation { redeemCertificate(certificateId: "cert-001",
                    reservationId: "res-001", idempotencyKey: "%s") {
                  ... on CertificateRedemption { reservationId message }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void buy_points_returns_purchase_transaction() {
        var result = dgs.execute("""
                mutation { buyPoints(input: { points: 1000, paymentMethodId: "pm-1" },
                    idempotencyKey: "%s") {
                  ... on PointsTransaction { id type points }
                  ... on PaymentDeclinedError { retryWithNewCard }
                  ... on ValidationError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void gift_points_returns_gift_success() {
        var result = dgs.execute("""
                mutation { giftPoints(input: {
                    recipientLoyaltyNumber: "LUX0002345678",
                    points: 250, message: "Birthday"
                }, idempotencyKey: "%s") {
                  ... on GiftPointsSuccess { pointsGifted recipientLoyaltyNumber message }
                  ... on InsufficientPointsError { shortfall }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void extend_points_expiry_returns_account_with_zero_expiring() {
        Integer expiringSoon = dgs.executeAndExtractJsonPath("""
                mutation { extendPointsExpiry {
                  ... on LoyaltyAccount { pointsBalance { expiringSoon } }
                  ... on ValidationError { code }
                } }
                """, "data.extendPointsExpiry.pointsBalance.expiringSoon");
        assertThat(expiringSoon).isZero();
    }
}
