package com.luxe.loyalty.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LoyaltyDataFetcherTest {

    @Autowired
    DgsQueryExecutor dgs;

    @Test
    void points_valuation_for_usd_returns_cash_money() {
        String currency = dgs.executeAndExtractJsonPath(
                """
                { pointsValuation(points: 50000, currency: "USD") {
                    points currency cashValue { amount currency } bestUse
                } }
                """,
                "data.pointsValuation.cashValue.currency");
        assertThat(currency).isEqualTo("USD");
    }

    @Test
    void available_challenges_query_returns_active_set() {
        List<Map<String, Object>> challenges = dgs.executeAndExtractJsonPath(
                "{ availableChallenges { id name status } }", "data.availableChallenges");
        assertThat(challenges).isNotEmpty();
    }

    @Test
    void loyalty_partners_query_returns_partners() {
        List<Map<String, Object>> partners = dgs.executeAndExtractJsonPath(
                "{ loyaltyPartners { edges { node { id name category } } } }",
                "data.loyaltyPartners.edges");
        assertThat(partners).isNotEmpty();
    }

    @Test
    void federation_entities_resolves_loyalty_account_by_key() {
        // first find a real account id
        // (we use the test data - 'lac-001' is seeded)
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on LoyaltyAccount { id loyaltyNumber tier }
                  }
                }
                """;
        var rep = Map.of("__typename", "LoyaltyAccount", "id", "lac-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void federation_entities_resolves_guest_profile_extension() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on GuestProfile { loyaltyAccount { tier } }
                  }
                }
                """;
        var rep = Map.of("__typename", "GuestProfile", "id", "guest-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
    }
}
