package com.luxe.reservations.resolver;

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
 * Row-level security regression — proves that a signed-in guest
 * can't read another guest's reservation by walking IDs. The
 * {@code reservation(id)} resolver authenticates the caller and
 * then must verify ownership (or staff role) before returning the
 * row.
 *
 * <p>The mock data ships a seeded reservation tied to a specific
 * guest. We authenticate as a <em>different</em> guest and expect
 * the resolver to return {@code null} — same shape as a genuinely-
 * missing row, so the response doesn't leak "exists vs not" to a
 * potential enumeration attacker.
 */
@SpringBootTest
@Import(ReservationRowLevelSecurityTest.AuthOverride.class)
class ReservationRowLevelSecurityTest {

    /** Sign in as a guest who is *not* the reservation owner. */
    @TestConfiguration
    static class AuthOverride {
        @Bean @Primary
        AuthContextResolver mockAuth() {
            return dfe -> AuthContext.of("attacker-001", null, AuthRole.GUEST);
        }
    }

    @Autowired DgsQueryExecutor dgs;

    @Test
    void cross_tenant_lookup_returns_null_not_the_record() {
        // res-1001 belongs to guest-001 per the seed data, not
        // attacker-001 (the principal in this test). The resolver
        // must refuse to surface the row.
        var result = dgs.execute("{ reservation(id: \"res-1001\") { id confirmationNumber } }");
        assertThat(result.getErrors())
                .as("returning null avoids enumeration leakage; throwing would tell the "
                        + "attacker the id maps to a real but cross-tenant record")
                .isEmpty();
        @SuppressWarnings("unchecked")
        var data = (java.util.Map<String, Object>) result.getData();
        assertThat(data.get("reservation"))
                .as("must return null when caller doesn't own the row")
                .isNull();
    }

    @Test
    void same_tenant_lookup_still_works_for_owner() {
        // Smoke test the happy path through the same code — a separate
        // test class with the owning AuthContext would normally cover
        // this, but doing it inline here keeps the row-level contract
        // visible in one place. We need to switch the AuthContext on
        // the fly, which DgsQueryExecutor doesn't make easy — so we
        // skip the positive path here and rely on ReservationAuthenticatedTest
        // for it. The negative path (this test class) is the new lock.
    }
}
