package com.luxe.common.error;

import com.luxe.common.auth.AuthRole;

public record AuthorizationError(String code, String message, AuthRole requiredRole) {
    public AuthorizationError(String message, AuthRole requiredRole) {
        this("AUTHORIZATION_ERROR", message, requiredRole);
    }
}
