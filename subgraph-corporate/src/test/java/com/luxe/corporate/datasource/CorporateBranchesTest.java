package com.luxe.corporate.datasource;

import com.luxe.corporate.schema.types.CorporateTraveler;
import com.luxe.corporate.schema.types.NegotiatedRate;
import com.luxe.corporate.schema.types.TravelApproval;
import com.luxe.corporate.schema.types.TravelPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Branch coverage for filter edge cases, traveler add/remove, approval review,
 * and policy update with mixed input shapes.
 */
class CorporateBranchesTest {

    private CorporateMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new CorporateMockDataSource();
    }

    // ── Negotiated rates filter branches ─────────────────────────────────────

    @Test
    void find_rates_with_null_hotel_id_returns_all_rates() {
        List<NegotiatedRate> all = ds.findRates("corp-001", null);
        assertThat(all).isNotEmpty();
    }

    @Test
    void find_rates_for_unknown_account_returns_empty() {
        assertThat(ds.findRates("corp-not-real", null)).isEmpty();
    }

    @Test
    void find_policy_for_unknown_account_is_empty() {
        assertThat(ds.findPolicy("corp-not-real")).isEmpty();
    }

    // ── Approval review branches ─────────────────────────────────────────────

    @Test
    void review_approval_unknown_id_returns_null() {
        assertThat(ds.reviewApproval("apr-not-real", "APPROVE", "tester", null))
                .isNull();
    }

    @Test
    void find_approval_unknown_id_is_empty() {
        assertThat(ds.findApproval("apr-not-real")).isEmpty();
    }

    @Test
    void pending_approvals_with_specific_account_filters_to_that_account() {
        List<TravelApproval> mine = ds.pendingApprovals("corp-001");
        assertThat(mine).allSatisfy(a ->
                assertThat(a.getAccountId()).isEqualTo("corp-001"));
    }

    // ── Traveler add/remove ──────────────────────────────────────────────────

    @Test
    void add_traveler_persists_traveler_and_links_guest_to_account() {
        CorporateTraveler t = ds.addTraveler(Map.of(
                "accountId", "corp-001",
                "guestId", "guest-new",
                "employeeId", "EMP-NEW",
                "department", "Eng",
                "costCenter", "CC-1",
                "managerEmail", "mgr@x.example",
                "travelClass", "FIRST"));
        assertThat(t).isNotNull();
        assertThat(t.getEmployeeId()).isEqualTo("EMP-NEW");
        // After adding, the guest should be findable by guest id
        assertThat(ds.findByGuestId("guest-new")).isPresent();
    }

    @Test
    void add_traveler_with_default_class_uses_business() {
        CorporateTraveler t = ds.addTraveler(Map.of(
                "accountId", "corp-001",
                "guestId", "guest-default-class",
                "employeeId", "EMP-X"));
        assertThat(t.getTravelClass()).isEqualTo("BUSINESS");
    }

    @Test
    void add_traveler_unknown_account_returns_null() {
        assertThat(ds.addTraveler(Map.of(
                "accountId", "corp-not-real",
                "guestId", "g",
                "employeeId", "E"))).isNull();
    }

    @Test
    void remove_traveler_unknown_id_returns_false() {
        assertThat(ds.removeTraveler("trv-not-real")).isFalse();
    }

    @Test
    void remove_traveler_known_id_returns_true_and_deactivates() {
        CorporateTraveler added = ds.addTraveler(Map.of(
                "accountId", "corp-001",
                "guestId", "guest-remove-me",
                "employeeId", "EMP-RM"));
        assertThat(ds.removeTraveler(added.getId())).isTrue();
        assertThat(added.isActive()).isFalse();
    }

    // ── Update policy branches ───────────────────────────────────────────────

    @Test
    void update_policy_with_rate_caps_replaces_them() {
        TravelPolicy p = ds.updatePolicy("corp-001", Map.of(
                "rateCaps", List.of(Map.of(
                        "city", "Tokyo",
                        "countryCode", "JP",
                        "maxNightlyRateUsd", 555.0))));
        assertThat(p.getRateCaps()).anySatisfy(c ->
                assertThat(c.city()).isEqualTo("Tokyo"));
    }

    @Test
    void update_policy_with_approval_chain_replaces_chain() {
        TravelPolicy p = ds.updatePolicy("corp-001", Map.of(
                "approvalChain", List.of(Map.of(
                        "level", 1,
                        "thresholdUsd", 1000.0,
                        "approverRole", "MANAGER",
                        "approverEmail", "mgr@example.com"))));
        assertThat(p.getApprovalChain()).anySatisfy(level ->
                assertThat(level.approverRole()).isEqualTo("MANAGER"));
    }

    @Test
    void update_policy_with_blocked_and_preferred_hotel_ids_persists_them() {
        TravelPolicy p = ds.updatePolicy("corp-001", Map.of(
                "blockedHotelIds", List.of("prop-blocked-001"),
                "preferredHotelIds", List.of("prop-paris-001", "prop-tokyo-001")));
        assertThat(p.getBlockedHotels()).anySatisfy(h ->
                assertThat(h.get("id")).isEqualTo("prop-blocked-001"));
        assertThat(p.getPreferredHotels()).hasSize(2);
    }

    @Test
    void update_policy_with_all_scalar_overrides_applied() {
        TravelPolicy p = ds.updatePolicy("corp-001", Map.of(
                "advanceBookingRequired", false,
                "advanceBookingDays", 0,
                "requiresBusinessJustification", false,
                "perDiemMealsUsd", 50.0,
                "allowedRoomCategories", List.of("STANDARD")));
        assertThat(p.isAdvanceBookingRequired()).isFalse();
        assertThat(p.isRequiresBusinessJustification()).isFalse();
        assertThat(p.getAllowedRoomCategories()).containsExactly("STANDARD");
        assertThat(p.getPerDiemMeals().amount()).isEqualTo("50.00");
    }

    @Test
    void update_policy_unknown_account_returns_null() {
        assertThat(ds.updatePolicy("corp-not-real", Map.of("maxNightlyRateUsd", 1.0)))
                .isNull();
    }

    // ── Travel report shape ──────────────────────────────────────────────────

    @Test
    void report_for_unknown_account_still_returns_well_typed_report() {
        var r = ds.report("corp-not-real", "MTD");
        assertThat(r).isNotNull();
        assertThat(r.totalSpend()).isNotNull();
        assertThat(r.spendByMonth()).isNotEmpty();
    }
}
