package com.luxe.corporate.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolver coverage for corporate mutations + remaining queries through DGS.
 */
@SpringBootTest
class CorporateMutationsResolverTest {

    @Autowired
    DgsQueryExecutor dgs;

    // ── Mutations: all require auth ──────────────────────────────────────────

    @Test
    void enroll_corporate_requires_auth() {
        String mutation = """
                mutation { enrollCorporate(input: {
                    companyName: "New Co",
                    industry: "Tech",
                    primaryContact: { name: "Owner", email: "owner@newco.example" },
                    contractStartDate: "2026-09-01",
                    contractEndDate: "2027-08-31"
                }) {
                  ... on CorporateAccount { id companyName }
                  ... on ValidationError { code }
                  ... on DuplicateContractError { existingContractNumber }
                } }
                """;
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void update_travel_policy_requires_auth() {
        String mutation = """
                mutation { updateTravelPolicy(accountId: "corp-001", input: {
                    maxNightlyRateUsd: 999.99,
                    requiresApprovalAboveUsd: 1500.0,
                    advanceBookingRequired: true,
                    advanceBookingDays: 14,
                    requiresBusinessJustification: true
                }) {
                  ... on TravelPolicy { id maxNightlyRateUsd { amount currency } }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """;
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void add_corporate_traveler_requires_auth() {
        String mutation = """
                mutation { addCorporateTraveler(input: {
                    accountId: "corp-001", guestId: "guest-001",
                    employeeId: "EMP-1", department: "Eng", costCenter: "CC-1",
                    travelClass: BUSINESS
                }) {
                  ... on CorporateTraveler { id employeeId }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """;
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void remove_corporate_traveler_requires_auth() {
        var result = dgs.execute(
                "mutation { removeCorporateTraveler(travelerId: \"trv-001\") }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void review_travel_approval_requires_auth() {
        String mutation = """
                mutation { reviewTravelApproval(approvalId: "apr-001",
                                                 decision: APPROVE,
                                                 notes: "OK") {
                  ... on TravelApproval { id status }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """;
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    // ── Auth-gated queries ───────────────────────────────────────────────────

    @Test
    void travel_report_requires_auth() {
        var result = dgs.execute(
                "{ travelReport(accountId: \"corp-001\", period: YTD) { totalTrips } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void pending_approvals_requires_auth() {
        var result = dgs.execute(
                "{ pendingApprovals(accountId: \"corp-001\") { id status } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void travel_policy_query_requires_auth() {
        var result = dgs.execute(
                "{ travelPolicy(accountId: \"corp-001\") { id requiresBusinessJustification } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void negotiated_rates_requires_auth() {
        var result = dgs.execute(
                "{ negotiatedRates(accountId: \"corp-001\") { id rateName discountPercent } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void corporate_account_by_id_requires_property_staff_role() {
        var result = dgs.execute(
                "{ corporateAccount(id: \"corp-001\") { id } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    // ── Federation entity resolution ─────────────────────────────────────────

    @Test
    void federation_entities_resolves_corporate_account_with_nested_fields() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on CorporateAccount {
                      id companyName tier
                      primaryContact { name email title }
                      travelers { id employeeId }
                      travelPolicy { id requiresBusinessJustification }
                      negotiatedRates { id rateName discountPercent }
                    }
                  }
                }
                """;
        var rep = Map.of("__typename", "CorporateAccount", "id", "corp-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities =
                (List<Map<String, Object>>) ((Map<String, Object>) result.getData()).get("_entities");
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).get("companyName")).isEqualTo("Acme Pharma");
    }

    @Test
    void federation_entities_resolves_unknown_corporate_account_returns_null() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on CorporateAccount { id }
                  }
                }
                """;
        var rep = Map.of("__typename", "CorporateAccount", "id", "corp-not-real");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        @SuppressWarnings("unchecked")
        List<Object> entities =
                (List<Object>) ((Map<String, Object>) result.getData()).get("_entities");
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0)).isNull();
    }

    @Test
    void federation_entities_resolves_guest_profile_with_corporate_extension() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on GuestProfile {
                      corporateAccount { id companyName tier }
                    }
                  }
                }
                """;
        var rep = Map.of("__typename", "GuestProfile", "id", "guest-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities =
                (List<Map<String, Object>>) ((Map<String, Object>) result.getData()).get("_entities");
        assertThat(entities).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> corp = (Map<String, Object>) entities.get(0).get("corporateAccount");
        assertThat(corp).isNotNull();
        assertThat(corp.get("id")).isEqualTo("corp-001");
    }

    @Test
    void federation_entities_resolves_guest_profile_without_corporate_returns_null() {
        // guest-006 isn't a traveler at any account
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on GuestProfile { corporateAccount { id } }
                  }
                }
                """;
        var rep = Map.of("__typename", "GuestProfile", "id", "guest-006");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities =
                (List<Map<String, Object>>) ((Map<String, Object>) result.getData()).get("_entities");
        assertThat(entities.get(0).get("corporateAccount")).isNull();
    }
}
