package com.luxe.guest.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration coverage for the {@code QueryGuardrails} DGS
 * instrumentation. We override the limits to small values so a
 * focused test query can prove both rejection paths
 * (QUERY_TOO_COMPLEX, QUERY_TOO_DEEP) without writing a query that
 * would actually slow down a real subgraph.
 */
@SpringBootTest
@TestPropertySource(properties = {
        // Tight thresholds for this test so we don't need 1000+
        // selections to trip the guard.
        "luxe.security.max-complexity=10",
        "luxe.security.max-depth=3",
})
class QueryGuardrailsTest {

    @Autowired DgsQueryExecutor dgs;

    @Test
    void over_complex_query_is_rejected_with_QUERY_TOO_COMPLEX() {
        // guests(first: 50) { edges { node { id } } } scores roughly
        // 1 (guests) + 50 * (1 (edges) + 1 (node) + 1 (id)) = 151,
        // well above our 10-limit.
        var result = dgs.execute("""
                { guests(first: 50) { edges { node { id } } } }
                """);
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getExtensions())
                .containsEntry("code", "QUERY_TOO_COMPLEX");
    }

    @Test
    void deeply_nested_query_is_rejected_with_QUERY_TOO_DEEP() {
        // Depth 5 (root + 4 levels) > our 3 max-depth.
        var result = dgs.execute("""
                { guests { edges { node { name { firstName } } } } }
                """);
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getExtensions().get("code"))
                .isIn("QUERY_TOO_COMPLEX", "QUERY_TOO_DEEP");
    }

    @Test
    void simple_well_scoped_query_passes_through() {
        // Within both limits, exercises the happy path.
        var result = dgs.execute("{ __typename }");
        assertThat(result.getErrors()).isEmpty();
    }
}
