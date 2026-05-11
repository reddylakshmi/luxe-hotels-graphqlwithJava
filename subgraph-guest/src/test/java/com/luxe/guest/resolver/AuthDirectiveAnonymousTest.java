package com.luxe.guest.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for the {@code @auth} field-level directive —
 * anonymous principal. Confirms that unauthenticated requests for
 * auth-gated fields return UNAUTHORIZED (with the right extensions
 * code), and that public fields on the same parent type remain
 * reachable. The directive is registered in
 * {@code common/auth/AuthDirectiveConfig}.
 */
@SpringBootTest
class AuthDirectiveAnonymousTest {

    private static final String GUEST_ID = "guest-001";

    @Autowired DgsQueryExecutor dgs;

    @Test
    void anonymous_read_of_guest_dateOfBirth_returns_unauthorized() {
        var result = dgs.execute("""
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on GuestProfile { id dateOfBirth }
                  }
                }
                """, Map.of("reps", List.of(
                        Map.of("__typename", "GuestProfile", "id", GUEST_ID))));
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getExtensions())
                .containsEntry("code", "UNAUTHORIZED");
    }

    @Test
    void anonymous_read_of_public_fields_succeeds() {
        // Confirms the directive is selective — id + name are NOT
        // gated, so a public federation lookup works fine.
        var result = dgs.execute("""
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on GuestProfile { id name { firstName } }
                  }
                }
                """, Map.of("reps", List.of(
                        Map.of("__typename", "GuestProfile", "id", GUEST_ID))));
        assertThat(result.getErrors()).isEmpty();
    }
}
