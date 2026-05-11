package com.luxe.guest.resolver;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for {@code @auth} — ADMIN role. ADMIN sits at
 * the top of the role hierarchy (GUEST &lt; LOYALTY_MEMBER &lt;
 * PROPERTY_STAFF &lt; REVENUE_MGR &lt; ADMIN), so it should pass both
 * GUEST-gated and ADMIN-gated fields.
 */
@SpringBootTest
@Import(AuthDirectiveAsAdminTest.AuthOverride.class)
class AuthDirectiveAsAdminTest {

    private static final String GUEST_ID = "guest-001";

    @TestConfiguration
    static class AuthOverride {
        @Bean @Primary
        AuthContextResolver mockAuth() {
            return dfe -> AuthContext.of("admin-001", null, AuthRole.ADMIN);
        }
    }

    @Autowired DgsQueryExecutor dgs;

    @Test
    void admin_can_read_admin_gated_pspToken() {
        var result = dgs.execute("""
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on GuestProfile {
                      paymentMethods(first: 1) { edges { node { id pspToken } } }
                    }
                  }
                }
                """, Map.of("reps", List.of(
                        Map.of("__typename", "GuestProfile", "id", GUEST_ID))));
        assertThat(result.getErrors()).isEmpty();
    }
}
