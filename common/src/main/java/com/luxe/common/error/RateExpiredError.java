package com.luxe.common.error;

public record RateExpiredError(String code, String message) {
    public RateExpiredError(String message) {
        this("RATE_EXPIRED", message);
    }
}
