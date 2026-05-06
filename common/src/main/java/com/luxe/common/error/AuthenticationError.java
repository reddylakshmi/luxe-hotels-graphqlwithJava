package com.luxe.common.error;

public record AuthenticationError(String code, String message) {
    public AuthenticationError(String message) {
        this("AUTHENTICATION_ERROR", message);
    }
}
