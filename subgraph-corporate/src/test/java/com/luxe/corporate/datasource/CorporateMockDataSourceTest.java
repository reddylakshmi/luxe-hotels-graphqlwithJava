package com.luxe.corporate.datasource;

import com.luxe.corporate.schema.types.CorporateAccount;
import com.luxe.corporate.schema.types.NegotiatedRate;
import com.luxe.corporate.schema.types.TravelApproval;
import com.luxe.corporate.schema.types.TravelPolicy;
import com.luxe.corporate.schema.types.TravelReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorporateMockDataSourceTest {

    private CorporateMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new CorporateMockDataSource();
    }

    @Test
    void seeded_accounts_include_known_companies() {
        assertThat(ds.findById("corp-001")).isPresent();
        assertThat(ds.findById("corp-002")).isPresent();
    }

    @Test
    void find_by_guest_id_resolves_traveler_to_their_account() {
        // guest-001 is a traveler at corp-001 (seeded)
        assertThat(ds.findByGuestId("guest-001"))
                .isPresent().get().extracting(CorporateAccount::getId).isEqualTo("corp-001");
    }

    @Test
    void find_by_unknown_guest_returns_empty() {
        assertThat(ds.findByGuestId("not-a-traveler")).isEmpty();
    }

    @Test
    void find_by_contract_number_round_trips() {
        CorporateAccount any = ds.findById("corp-001").orElseThrow();
        assertThat(ds.findByContractNumber(any.getContractNumber()))
                .isPresent().get().extracting(CorporateAccount::getId).isEqualTo("corp-001");
    }

    @Test
    void find_policy_returns_account_policy() {
        TravelPolicy policy = ds.findPolicy("corp-001").orElseThrow();
        assertThat(policy.getApprovalChain()).isNotEmpty();
        assertThat(policy.getRateCaps()).isNotEmpty();
    }

    @Test
    void find_rates_filters_by_hotel_id() {
        List<NegotiatedRate> paris = ds.findRates("corp-001", "prop-paris-001");
        assertThat(paris).allSatisfy(r -> assertThat(r.hotelId()).isEqualTo("prop-paris-001"));
    }

    @Test
    void enroll_creates_account_in_pending_review() {
        CorporateAccount a = ds.enroll(Map.of(
                "companyName", "New Co",
                "industry", "Tech",
                "primaryContact", Map.of("name", "X", "email", "x@nco.example"),
                "contractStartDate", "2026-06-01",
                "contractEndDate", "2027-05-31"));
        assertThat(a.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(a.getCompanyName()).isEqualTo("New Co");
    }

    @Test
    void update_policy_applies_max_rate_change() {
        ds.updatePolicy("corp-001", Map.of("maxNightlyRateUsd", 999.99));
        assertThat(ds.findPolicy("corp-001").orElseThrow()
                .getMaxNightlyRateUsd().amount()).isEqualTo("999.99");
    }

    @Test
    void pending_approvals_lists_pending_only() {
        List<TravelApproval> pending = ds.pendingApprovals(null);
        assertThat(pending).isNotEmpty();
        assertThat(pending).allSatisfy(a -> assertThat(a.getStatus()).isEqualTo("PENDING"));
    }

    @Test
    void review_approval_with_approve_decision_marks_approved() {
        TravelApproval a = ds.reviewApproval("apr-001", "APPROVE", "tester", "ok");
        assertThat(a.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void review_approval_with_deny_decision_marks_denied() {
        TravelApproval a = ds.reviewApproval("apr-002", "DENY", "tester", "no");
        assertThat(a.getStatus()).isEqualTo("DENIED");
    }

    @Test
    void travel_report_returns_summary_for_account() {
        TravelReport r = ds.report("corp-001", "YTD");
        assertThat(r.totalTrips()).isPositive();
        assertThat(r.policyComplianceRate()).isBetween(0.0, 1.0);
        assertThat(r.spendByMonth()).isNotEmpty();
        assertThat(r.topDestinations()).isNotEmpty();
    }
}
