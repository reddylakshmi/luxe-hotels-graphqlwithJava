package com.luxe.loyalty.datasource;

import com.luxe.loyalty.schema.types.LoyaltyAccount;
import com.luxe.loyalty.schema.types.LoyaltyPartner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoyaltyMockDataSourceTest {

    private LoyaltyMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new LoyaltyMockDataSource();
    }

    @Test
    void seeded_accounts_span_all_tiers() {
        // We seeded MEMBER through AMBASSADOR
        var byGuestOne = ds.findByGuestId("guest-001");
        assertThat(byGuestOne).isPresent();
        assertThat(byGuestOne.get().getTier()).isEqualTo("GOLD");
    }

    @Test
    void find_by_loyalty_number_resolves_known_number() {
        LoyaltyAccount a = ds.findByGuestId("guest-001").orElseThrow();
        assertThat(ds.findByLoyaltyNumber(a.getLoyaltyNumber()))
                .isPresent().get().extracting(LoyaltyAccount::getId).isEqualTo(a.getId());
    }

    @Test
    void find_by_id_returns_empty_for_unknown() {
        assertThat(ds.findById("not-a-thing")).isEmpty();
    }

    @Test
    void points_valuation_returns_cash_equivalent_in_requested_currency() {
        var v = ds.valuePoints(50_000, "USD");
        assertThat(v.points()).isEqualTo(50_000);
        assertThat(v.currency()).isEqualTo("USD");
        assertThat(v.cashValue().currency()).isEqualTo("USD");
        assertThat(v.cashValue().amount()).isNotBlank();
    }

    @Test
    void find_partners_returns_seeded_partners() {
        assertThat(ds.findPartners(null)).isNotEmpty();
    }

    @Test
    void find_partners_filters_by_categories() {
        var airlines = ds.findPartners(java.util.List.of("AIRLINE"));
        assertThat(airlines).isNotEmpty();
        assertThat(airlines).allSatisfy(p ->
                assertThat(p.category()).isEqualTo("AIRLINE"));
    }

    @Test
    void find_partner_by_known_id_returns_partner() {
        LoyaltyPartner any = ds.findPartners(null).get(0);
        assertThat(ds.findPartnerById(any.id()))
                .isPresent().get().extracting(LoyaltyPartner::id).isEqualTo(any.id());
    }

    @Test
    void available_challenges_includes_active_or_upcoming_only() {
        var open = ds.findAvailableChallenges();
        assertThat(open).isNotEmpty();
        assertThat(open).allSatisfy(c -> assertThat(c.getStatus())
                .isIn("ACTIVE", "UPCOMING"));
    }

    @Test
    void enroll_creates_member_tier_account() {
        LoyaltyAccount enrolled = ds.enroll("brand-new-guest", null, true);
        assertThat(enrolled.getTier()).isEqualTo("MEMBER");
        assertThat(enrolled.getGuestId()).isEqualTo("brand-new-guest");
    }

    @Test
    void buy_points_increments_available_balance() {
        LoyaltyAccount before = ds.findByGuestId("guest-001").orElseThrow();
        int beforeBalance = before.getPointsAvailable();
        ds.buyPoints(before.getId(), 1_000, "pm-1");
        assertThat(before.getPointsAvailable()).isEqualTo(beforeBalance + 1_000);
    }

    @Test
    void benefits_for_tier_returns_per_tier_set() {
        assertThat(ds.benefitsForTier("AMBASSADOR")).isNotEmpty();
        assertThat(ds.benefitsForTier("UNKNOWN")).isEmpty();
    }
}
