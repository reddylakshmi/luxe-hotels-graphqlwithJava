package com.luxe.common.error;

public record NotFoundError(String code, String message, String resourceType) {
    public NotFoundError(String resourceType, String id) {
        this("NOT_FOUND", resourceType + " not found: " + id, resourceType);
    }
}
