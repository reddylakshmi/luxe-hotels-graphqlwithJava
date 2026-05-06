package com.luxe.common.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-256-bits-long-or-greater";
    private final JwtService service = new JwtService(SECRET, 3_600_000L);

    @Test
    void generated_token_round_trips_to_same_auth_context() {
        String token = service.generateToken("guest-001", "LUX0001234567", AuthRole.GUEST);
        AuthContext ctx = service.parseToken(token);

        assertThat(ctx.isAuthenticated()).isTrue();
        assertThat(ctx.guestId()).isEqualTo("guest-001");
        assertThat(ctx.loyaltyNumber()).isEqualTo("LUX0001234567");
        assertThat(ctx.role()).isEqualTo(AuthRole.GUEST);
    }

    @Test
    void generated_token_is_valid() {
        String token = service.generateToken("guest-001", null, AuthRole.GUEST);
        assertThat(service.isValid(token)).isTrue();
    }

    @Test
    void invalid_token_signature_returns_anonymous_context() {
        String forged = service.generateToken("guest-001", null, AuthRole.GUEST) + "tampered";
        AuthContext ctx = service.parseToken(forged);
        assertThat(ctx.isAuthenticated()).isFalse();
    }

    @Test
    void invalid_token_returns_false_from_is_valid() {
        assertThat(service.isValid("not-a-real-token")).isFalse();
        assertThat(service.isValid("")).isFalse();
    }

    @Test
    void token_signed_with_different_secret_is_rejected() {
        JwtService other = new JwtService("a-completely-different-secret-of-sufficient-length", 60_000L);
        String fromOther = other.generateToken("guest-001", null, AuthRole.GUEST);
        assertThat(service.isValid(fromOther)).isFalse();
        assertThat(service.parseToken(fromOther).isAuthenticated()).isFalse();
    }

    @Test
    void mock_jwt_with_simple_id_and_role_parses() {
        AuthContext ctx = service.parseToken("mock-jwt-admin-ADMIN");
        assertThat(ctx.isAuthenticated()).isTrue();
        assertThat(ctx.guestId()).isEqualTo("admin");
        assertThat(ctx.role()).isEqualTo(AuthRole.ADMIN);
    }

    @Test
    void mock_jwt_with_hyphenated_id_reconstructs_full_id() {
        AuthContext ctx = service.parseToken("mock-jwt-guest-001-GUEST");
        assertThat(ctx.isAuthenticated()).isTrue();
        assertThat(ctx.guestId()).isEqualTo("guest-001");
        assertThat(ctx.role()).isEqualTo(AuthRole.GUEST);
    }

    @Test
    void mock_jwt_with_unknown_role_falls_back_to_guest() {
        AuthContext ctx = service.parseToken("mock-jwt-someone-WIZARD");
        assertThat(ctx.role()).isEqualTo(AuthRole.GUEST);
    }

    @Test
    void mock_jwt_with_property_staff_role_parses() {
        AuthContext ctx = service.parseToken("mock-jwt-staff-jane-PROPERTY_STAFF");
        assertThat(ctx.guestId()).isEqualTo("staff-jane");
        assertThat(ctx.role()).isEqualTo(AuthRole.PROPERTY_STAFF);
    }

    @Test
    void generated_token_with_admin_role_parses_back_to_admin() {
        String token = service.generateToken("admin-1", null, AuthRole.ADMIN);
        assertThat(service.parseToken(token).role()).isEqualTo(AuthRole.ADMIN);
    }

    @Test
    void already_expired_token_returns_anonymous() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, 1L); // 1ms expiry
        String token = shortLived.generateToken("guest-001", null, AuthRole.GUEST);
        Thread.sleep(15);
        assertThat(shortLived.isValid(token)).isFalse();
        assertThat(shortLived.parseToken(token).isAuthenticated()).isFalse();
    }
}
