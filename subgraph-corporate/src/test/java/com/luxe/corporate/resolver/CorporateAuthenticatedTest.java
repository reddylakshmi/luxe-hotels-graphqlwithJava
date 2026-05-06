package com.luxe.corporate.resolver;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authenticated coverage of corporate mutations + auth-gated queries.
 */
@SpringBootTest
@Import(CorporateAuthenticatedTest.AuthOverrideConfig.class)
class CorporateAuthenticatedTest {

    @TestConfiguration
    static class AuthOverrideConfig {
        @Bean @Primary
        AuthContextResolver mockAuth() {
            return dfe -> AuthContext.of("guest-001", "LUX0001234567", AuthRole.ADMIN);
        }
    }

    @Autowired
    DgsQueryExecutor dgs;

    // ── Auth-gated queries ───────────────────────────────────────────────────

    @Test
    void my_corporate_account_returns_account_for_authenticated_traveler() {
        // guest-001 is seeded as a traveler at corp-001 (Acme)
        String id = dgs.executeAndExtractJsonPath(
                "{ myCorporateAccount { id companyName tier } }",
                "data.myCorporateAccount.id");
        assertThat(id).isEqualTo("corp-001");
    }

    @Test
    void corporate_account_by_id_returns_account_with_admin_role() {
        String name = dgs.executeAndExtractJsonPath(
                "{ corporateAccount(id: \"corp-002\") { id companyName tier } }",
                "data.corporateAccount.companyName");
        assertThat(name).isEqualTo("Greenfield Capital");
    }

    @Test
    void travel_report_returns_report_summary() {
        Integer trips = dgs.executeAndExtractJsonPath(
                "{ travelReport(accountId: \"corp-001\", period: YTD) {"
                        + " totalTrips totalSpend { amount currency }"
                        + " policyComplianceRate spendByMonth { month trips } } }",
                "data.travelReport.totalTrips");
        assertThat(trips).isPositive();
    }

    @Test
    void pending_approvals_returns_list() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ pendingApprovals(accountId: \"corp-001\") { id status totalAmount { amount } } }",
                "data.pendingApprovals");
        assertThat(result).isNotNull();
    }

    @Test
    void travel_policy_returns_policy() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ travelPolicy(accountId: \"corp-001\") { id requiresBusinessJustification"
                        + " approvalChain { level approverRole } rateCaps { city countryCode } } }",
                "data.travelPolicy");
        assertThat(result).isNotNull();
    }

    @Test
    void negotiated_rates_returns_rates() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ negotiatedRates(accountId: \"corp-001\") { id rateName discountPercent } }",
                "data.negotiatedRates");
        assertThat(result).isNotNull();
    }

    @Test
    void negotiated_rates_filter_by_hotel() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ negotiatedRates(accountId: \"corp-001\", propertyId: \"prop-paris-001\") {"
                        + " hotelId rateName } }",
                "data.negotiatedRates");
        assertThat(result).isNotNull();
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    @Test
    void enroll_corporate_creates_account_in_pending_review() {
        String status = dgs.executeAndExtractJsonPath("""
                mutation { enrollCorporate(input: {
                    companyName: "New Corp",
                    industry: "Tech",
                    primaryContact: { name: "Alice", email: "alice@new.example" },
                    contractStartDate: "2026-09-01",
                    contractEndDate: "2027-08-31",
                    tier: STANDARD
                }) {
                  __typename
                  ... on CorporateAccount { status }
                  ... on ValidationError { code }
                  ... on DuplicateContractError { existingContractNumber }
                } }
                """, "data.enrollCorporate.status");
        assertThat(status).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void update_travel_policy_returns_updated_policy() {
        String amount = dgs.executeAndExtractJsonPath("""
                mutation { updateTravelPolicy(accountId: "corp-001", input: {
                    maxNightlyRateUsd: 850.50,
                    requiresApprovalAboveUsd: 1200.0,
                    advanceBookingRequired: true,
                    advanceBookingDays: 21,
                    requiresBusinessJustification: true,
                    perDiemMealsUsd: 95.0
                }) {
                  ... on TravelPolicy { id maxNightlyRateUsd { amount } }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """, "data.updateTravelPolicy.maxNightlyRateUsd.amount");
        assertThat(amount).isEqualTo("850.50");
    }

    @Test
    void update_travel_policy_unknown_account_returns_not_found() {
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { updateTravelPolicy(accountId: "corp-not-real",
                    input: { maxNightlyRateUsd: 100.0 }) {
                  __typename
                  ... on TravelPolicy { id }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """, "data.updateTravelPolicy.__typename");
        assertThat(typename).isEqualTo("NotFoundError");
    }

    @Test
    void add_corporate_traveler_returns_traveler() {
        String employeeId = dgs.executeAndExtractJsonPath("""
                mutation { addCorporateTraveler(input: {
                    accountId: "corp-001",
                    guestId: "guest-new-traveler",
                    employeeId: "EMP-NEW",
                    department: "Engineering",
                    travelClass: BUSINESS
                }) {
                  ... on CorporateTraveler { id employeeId department }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """, "data.addCorporateTraveler.employeeId");
        assertThat(employeeId).isEqualTo("EMP-NEW");
    }

    @Test
    void add_corporate_traveler_unknown_account_returns_not_found() {
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { addCorporateTraveler(input: {
                    accountId: "corp-not-real",
                    guestId: "guest-x", employeeId: "X"
                }) {
                  __typename
                  ... on CorporateTraveler { id }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """, "data.addCorporateTraveler.__typename");
        assertThat(typename).isEqualTo("NotFoundError");
    }

    @Test
    void remove_corporate_traveler_known_traveler_returns_true() {
        // first add then remove
        String tid = dgs.executeAndExtractJsonPath("""
                mutation { addCorporateTraveler(input: {
                    accountId: "corp-001", guestId: "guest-tmp",
                    employeeId: "EMP-TMP"
                }) { ... on CorporateTraveler { id } } }
                """, "data.addCorporateTraveler.id");
        Boolean removed = dgs.executeAndExtractJsonPath(
                "mutation { removeCorporateTraveler(travelerId: \"" + tid + "\") }",
                "data.removeCorporateTraveler");
        assertThat(removed).isTrue();
    }

    @Test
    void remove_unknown_traveler_returns_false() {
        Boolean removed = dgs.executeAndExtractJsonPath(
                "mutation { removeCorporateTraveler(travelerId: \"trv-not-real\") }",
                "data.removeCorporateTraveler");
        assertThat(removed).isFalse();
    }

    @Test
    void review_travel_approval_with_approve_decision_marks_approved() {
        String status = dgs.executeAndExtractJsonPath("""
                mutation { reviewTravelApproval(approvalId: "apr-001",
                    decision: APPROVE, notes: "OK to exceed cap once") {
                  __typename
                  ... on TravelApproval { id status decidedBy notes }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """, "data.reviewTravelApproval.status");
        assertThat(status).isEqualTo("APPROVED");
    }

    @Test
    void review_unknown_approval_returns_not_found() {
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { reviewTravelApproval(approvalId: "apr-not-real",
                    decision: DENY) {
                  __typename
                  ... on TravelApproval { id }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """, "data.reviewTravelApproval.__typename");
        assertThat(typename).isEqualTo("NotFoundError");
    }
}
