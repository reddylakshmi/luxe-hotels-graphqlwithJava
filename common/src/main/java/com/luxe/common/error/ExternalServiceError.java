package com.luxe.common.error;

public record ExternalServiceError(String code, String message, String service, boolean retryable) {
    public ExternalServiceError(String service, String message, boolean retryable) {
        this("EXTERNAL_SERVICE_ERROR", message, service, retryable);
    }
}
