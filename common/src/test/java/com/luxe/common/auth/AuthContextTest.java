package com.luxe.common.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthContextTest {

    @Test
    void anonymous_is_unauthenticated() {
        AuthContext ctx = AuthContext.anonymous();
        assertThat(ctx.isAuthenticated()).isFalse();
        assertThat(ctx.guestId()).isNull();
        assertThat(ctx.role()).isNull();
    }

    @Test
    void of_factory_marks_authenticated() {
        AuthContext ctx = AuthContext.of("g1", "LX1", AuthRole.GUEST);
        assertThat(ctx.isAuthenticated()).isTrue();
        assertThat(ctx.guestId()).isEqualTo("g1");
        assertThat(ctx.role()).isEqualTo(AuthRole.GUEST);
    }

    @Test
    void require_auth_throws_for_anonymous() {
        assertThatThrownBy(() -> AuthContext.anonymous().requireAuth())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    void require_auth_passes_when_authenticated() {
        assertThatNoException()
                .isThrownBy(() -> AuthContext.of("g1", null, AuthRole.GUEST).requireAuth());
    }

    @Test
    void require_role_throws_when_anonymous() {
        assertThatThrownBy(() -> AuthContext.anonymous().requireRole(AuthRole.GUEST))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void require_role_passes_when_role_meets_requirement() {
        assertThatNoException().isThrownBy(() ->
                AuthContext.of("u", null, AuthRole.ADMIN).requireRole(AuthRole.PROPERTY_STAFF));
        assertThatNoException().isThrownBy(() ->
                AuthContext.of("u", null, AuthRole.GUEST).requireRole(AuthRole.GUEST));
    }

    @Test
    void require_role_throws_when_role_below_requirement() {
        assertThatThrownBy(() -> AuthContext.of("u", null, AuthRole.GUEST)
                .requireRole(AuthRole.PROPERTY_STAFF))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Role PROPERTY_STAFF required");
    }

    @Test
    void has_role_is_true_when_authorized() {
        AuthContext ctx = AuthContext.of("u", null, AuthRole.REVENUE_MGR);
        assertThat(ctx.hasRole(AuthRole.GUEST)).isTrue();
        assertThat(ctx.hasRole(AuthRole.PROPERTY_STAFF)).isTrue();
        assertThat(ctx.hasRole(AuthRole.REVENUE_MGR)).isTrue();
        assertThat(ctx.hasRole(AuthRole.ADMIN)).isFalse();
    }

    @Test
    void has_role_is_false_for_anonymous() {
        assertThat(AuthContext.anonymous().hasRole(AuthRole.GUEST)).isFalse();
    }

    @Test
    void unauthorized_exception_carries_unauthorized_extension() {
        UnauthorizedException ex = new UnauthorizedException("nope");
        assertThat(ex.getMessage()).isEqualTo("nope");
        assertThat(ex.getExtensions()).containsEntry("code", "UNAUTHORIZED");
    }
}
