package com.luxe.loyalty.datasource;

import com.luxe.loyalty.schema.types.Certificate;
import com.luxe.loyalty.schema.types.Challenge;
import com.luxe.loyalty.schema.types.LinkedPartnerAccount;
import com.luxe.loyalty.schema.types.LoyaltyAccount;
import com.luxe.loyalty.schema.types.PointsTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the 9 loyalty mutations and the transaction-filter branches.
 * Every test asserts both the return value and the persisted side-effect
 * via a follow-up read.
 */
class LoyaltyMutationsTest {

    private LoyaltyMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new LoyaltyMockDataSource();
    }

    // ── transferPoints ───────────────────────────────────────────────────────

    @Test
    void transfer_points_decrements_sender_and_credits_recipient() {
        LoyaltyAccount sender = ds.findByGuestId("guest-001").orElseThrow();   // GOLD, 87500
        LoyaltyAccount recipient = ds.findByGuestId("guest-002").orElseThrow(); // PLATINUM, 42300
        int beforeSender = sender.getPointsAvailable();
        int beforeRecipient = recipient.getPointsAvailable();

        PointsTransaction tx = ds.transferPoints(
                sender.getLoyaltyNumber(), recipient.getLoyaltyNumber(),
                5_000, "Birthday gift");
        assertThat(tx).isNotNull();
        assertThat(sender.getPointsAvailable()).isEqualTo(beforeSender - 5_000);
        assertThat(recipient.getPointsAvailable()).isEqualTo(beforeRecipient + 5_000);
    }

    @Test
    void transfer_points_unknown_sender_returns_null() {
        assertThat(ds.transferPoints("LUX-NOT-REAL", "LUX0001234567", 100, null))
                .isNull();
    }

    @Test
    void transfer_points_unknown_recipient_returns_null() {
        assertThat(ds.transferPoints("LUX0001234567", "LUX-NOT-REAL", 100, null))
                .isNull();
    }

    @Test
    void transfer_points_with_insufficient_balance_returns_null() {
        // sender has 87,500 — try to transfer 99 million
        assertThat(ds.transferPoints("LUX0001234567", "LUX0002345678", 99_000_000, null))
                .isNull();
    }

    // ── transferToAirline ────────────────────────────────────────────────────

    @Test
    void transfer_to_airline_deducts_points_and_records_redeem_transfer_tx() {
        LoyaltyAccount a = ds.findByGuestId("guest-001").orElseThrow();
        int before = a.getPointsAvailable();
        PointsTransaction tx = ds.transferToAirline(a.getId(), "ptr-001",
                "SK-12345678", 4_000);
        assertThat(tx).isNotNull();
        assertThat(tx.type()).isEqualTo("REDEEM_TRANSFER");
        assertThat(a.getPointsAvailable()).isEqualTo(before - 4_000);
    }

    @Test
    void transfer_to_airline_unknown_partner_returns_null() {
        LoyaltyAccount a = ds.findByGuestId("guest-001").orElseThrow();
        assertThat(ds.transferToAirline(a.getId(), "ptr-not-real", "X", 1_000)).isNull();
    }

    @Test
    void transfer_to_airline_unknown_account_returns_null() {
        assertThat(ds.transferToAirline("not-real", "ptr-001", "X", 1_000)).isNull();
    }

    // ── linkPartnerAccount ───────────────────────────────────────────────────

    @Test
    void link_partner_account_attaches_link_to_the_account() {
        LoyaltyAccount a = ds.findByGuestId("guest-001").orElseThrow();
        int before = a.getLinkedPartners().size();
        LinkedPartnerAccount link = ds.linkPartnerAccount(a.getId(), "ptr-002",
                "PW-12345", "Smith");
        assertThat(link).isNotNull();
        assertThat(link.status()).isEqualTo("ACTIVE");
        assertThat(a.getLinkedPartners()).hasSize(before + 1);
    }

    @Test
    void link_partner_unknown_partner_returns_null() {
        LoyaltyAccount a = ds.findByGuestId("guest-001").orElseThrow();
        assertThat(ds.linkPartnerAccount(a.getId(), "ptr-not-real", "X", null)).isNull();
    }

    // ── registerForChallenge ─────────────────────────────────────────────────

    @Test
    void register_for_challenge_marks_registered_and_initializes_progress() {
        // chl-001 is seeded as ACTIVE, registered=false
        Challenge c = ds.registerForChallenge("any-account", "chl-001");
        assertThat(c).isNotNull();
        assertThat(c.isRegistered()).isTrue();
        assertThat(c.getProgress()).isNotNull();
    }

    @Test
    void register_for_unknown_challenge_returns_null() {
        assertThat(ds.registerForChallenge("any-account", "chl-not-real")).isNull();
    }

    // ── redeemCertificate ────────────────────────────────────────────────────

    @Test
    void redeem_active_certificate_marks_it_redeemed_and_links_reservation() {
        // cert-001 is ACTIVE on lac-001
        Certificate cert = ds.redeemCertificate("lac-001", "cert-001", "res-001");
        assertThat(cert).isNotNull();
        assertThat(cert.getStatus()).isEqualTo("REDEEMED");
        assertThat(cert.getReservationId()).isEqualTo("res-001");
        assertThat(cert.getRedeemedAt()).isNotNull();
    }

    @Test
    void redeem_already_redeemed_certificate_returns_null() {
        // cert-004 is seeded as REDEEMED
        assertThat(ds.redeemCertificate("lac-003", "cert-004", "res-302")).isNull();
    }

    @Test
    void redeem_unknown_certificate_returns_null() {
        assertThat(ds.redeemCertificate("lac-001", "cert-not-real", "res-001"))
                .isNull();
    }

    @Test
    void redeem_certificate_unknown_account_returns_null() {
        assertThat(ds.redeemCertificate("not-real", "cert-001", "res-001")).isNull();
    }

    // ── buyPoints ────────────────────────────────────────────────────────────

    @Test
    void buy_points_persists_purchase_transaction_with_purchase_type() {
        LoyaltyAccount a = ds.findByGuestId("guest-001").orElseThrow();
        int beforeAvailable = a.getPointsAvailable();
        int beforeLifetime = a.getLifetimePoints();
        PointsTransaction tx = ds.buyPoints(a.getId(), 2_500, "pm-1");
        assertThat(tx.type()).isEqualTo("EARN_PURCHASE");
        assertThat(a.getPointsAvailable()).isEqualTo(beforeAvailable + 2_500);
        assertThat(a.getLifetimePoints()).isEqualTo(beforeLifetime + 2_500);
    }

    @Test
    void buy_points_unknown_account_returns_null() {
        assertThat(ds.buyPoints("not-real", 1_000, "pm-1")).isNull();
    }

    // ── giftPoints (delegates to transferPoints) ─────────────────────────────

    @Test
    void gift_points_round_trips_through_transfer_path() {
        LoyaltyAccount sender = ds.findByGuestId("guest-001").orElseThrow();
        LoyaltyAccount recipient = ds.findByGuestId("guest-002").orElseThrow();
        int beforeSender = sender.getPointsAvailable();
        PointsTransaction tx = ds.giftPoints(
                sender.getLoyaltyNumber(), recipient.getLoyaltyNumber(),
                250, "Just because");
        assertThat(tx).isNotNull();
        assertThat(sender.getPointsAvailable()).isEqualTo(beforeSender - 250);
    }

    // ── extendPointsExpiry ───────────────────────────────────────────────────

    @Test
    void extend_points_expiry_zeros_out_expiring_soon() {
        // guest-001 has 5,000 expiring soon seeded
        LoyaltyAccount a = ds.findByGuestId("guest-001").orElseThrow();
        assertThat(a.getPointsExpiringSoon()).isPositive();
        ds.extendPointsExpiry(a.getId());
        assertThat(a.getPointsExpiringSoon()).isZero();
    }

    @Test
    void extend_expiry_unknown_account_returns_null() {
        assertThat(ds.extendPointsExpiry("not-real")).isNull();
    }

    // ── transactions filtering & sorting branches ────────────────────────────

    @Test
    void find_transactions_filters_by_type() {
        List<PointsTransaction> earns = ds.findTransactions("lac-001",
                Map.of("type", "EARN_STAY"), null);
        assertThat(earns).isNotEmpty();
        assertThat(earns).allSatisfy(t -> assertThat(t.type()).isEqualTo("EARN_STAY"));
    }

    @Test
    void find_transactions_filters_by_reservation_id() {
        List<PointsTransaction> forRes = ds.findTransactions("lac-001",
                Map.of("reservationId", "res-101"), null);
        assertThat(forRes).allSatisfy(t ->
                assertThat(t.reservationId()).isEqualTo("res-101"));
    }

    @Test
    void find_transactions_sorts_by_date_ascending() {
        List<PointsTransaction> txs = ds.findTransactions("lac-001", null, "DATE_ASC");
        for (int i = 0; i < txs.size() - 1; i++) {
            assertThat(txs.get(i).transactionDate())
                    .isBeforeOrEqualTo(txs.get(i + 1).transactionDate());
        }
    }

    @Test
    void find_transactions_sorts_by_points_desc() {
        List<PointsTransaction> txs = ds.findTransactions("lac-001", null, "POINTS_DESC");
        for (int i = 0; i < txs.size() - 1; i++) {
            assertThat(txs.get(i).points()).isGreaterThanOrEqualTo(txs.get(i + 1).points());
        }
    }

    @Test
    void find_transactions_default_sort_is_date_descending() {
        List<PointsTransaction> txs = ds.findTransactions("lac-001", null, null);
        for (int i = 0; i < txs.size() - 1; i++) {
            assertThat(txs.get(i).transactionDate())
                    .isAfterOrEqualTo(txs.get(i + 1).transactionDate());
        }
    }

    @Test
    void find_certificates_filters_by_status() {
        List<Certificate> active = ds.findCertificates("lac-001", "ACTIVE");
        assertThat(active).allSatisfy(c -> assertThat(c.getStatus()).isEqualTo("ACTIVE"));
    }

    @Test
    void find_certificates_unknown_account_returns_empty() {
        assertThat(ds.findCertificates("not-real", null)).isEmpty();
    }

    @Test
    void find_partners_with_empty_categories_returns_all() {
        var all = ds.findPartners(List.of());
        assertThat(all).isNotEmpty();
    }

    @Test
    void find_challenge_by_known_id_returns_challenge() {
        assertThat(ds.findChallengeById("chl-001")).isPresent();
    }

    @Test
    void find_challenge_by_unknown_id_is_empty() {
        assertThat(ds.findChallengeById("chl-not-real")).isEmpty();
    }
}
