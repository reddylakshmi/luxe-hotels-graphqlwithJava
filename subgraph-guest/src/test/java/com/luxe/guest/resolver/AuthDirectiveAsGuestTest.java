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
 * Regression coverage for {@code @auth} — authenticated GUEST role.
 * Passes GUEST-gated fields (dateOfBirth, phone, holderName) but is
 * forbidden from ADMIN-gated fields (pspToken, the vault key).
 */
@SpringBootTest
@Import(AuthDirectiveAsGuestTest.AuthOverride.class)
class AuthDirectiveAsGuestTest {

    private static final String GUEST_ID = "guest-001";

    @TestConfiguration
    static class AuthOverride {
        @Bean @Primary
        AuthContextResolver mockAuth() {
            return dfe -> AuthContext.of(GUEST_ID, "LUX0001234567", AuthRole.GUEST);
        }
    }

    @Autowired DgsQueryExecutor dgs;

    @Test
    void guest_can_read_guest_gated_dateOfBirth_and_phone() {
        var result = dgs.execute("""
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on GuestProfile { id dateOfBirth phone }
                  }
                }
                """, Map.of("reps", List.of(
                        Map.of("__typename", "GuestProfile", "id", GUEST_ID))));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void guest_cannot_read_admin_gated_pspToken() {
        // pspToken is @auth(requires: ADMIN) — the vault key, never
        // visible to guests even on their own payment methods. We
        // expect FORBIDDEN (not UNAUTHORIZED) because we ARE
        // authenticated, just under-privileged.
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
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getExtensions())
                .containsEntry("code", "FORBIDDEN");
    }
}
