package com.luxe.corporate.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CorporateDataFetcherTest {

    @Autowired
    DgsQueryExecutor dgs;

    @Test
    void my_corporate_account_requires_authentication() {
        var result = dgs.execute("{ myCorporateAccount { id } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void travel_policy_query_works() {
        var result = dgs.execute(
                "{ travelPolicy(accountId: \"corp-001\") { id maxNightlyRateUsd { amount currency } } }");
        // requires auth — should fail without a JWT
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void federation_entities_resolves_corporate_account_by_key() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on CorporateAccount { id companyName tier }
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
        assertThat(entities.get(0).get("id")).isEqualTo("corp-001");
    }
}
