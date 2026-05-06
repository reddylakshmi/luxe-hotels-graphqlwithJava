package com.luxe.common.auth;


public record AuthContext(
        String guestId,
        String loyaltyNumber,
        AuthRole role,
        boolean isAuthenticated
) {
    public static AuthContext anonymous() {
        return new AuthContext(null, null, null, false);
    }

    public static AuthContext of(String guestId, String loyaltyNumber, AuthRole role) {
        return new AuthContext(guestId, loyaltyNumber, role, true);
    }

    public void requireAuth() {
        if (!isAuthenticated) {
            throw new UnauthorizedException("Authentication required");
        }
    }

    public void requireRole(AuthRole required) {
        requireAuth();
        if (role == null || role.ordinal() < required.ordinal()) {
            throw new UnauthorizedException("Role " + required + " required, but user has " + role);
        }
    }

    public boolean hasRole(AuthRole required) {
        return isAuthenticated && role != null && role.ordinal() >= required.ordinal();
    }
}
