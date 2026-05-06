package com.luxe.common.error;

public record AlreadyEnrolledError(String code, String message, String existingLoyaltyNumber) {
    public AlreadyEnrolledError(String existingLoyaltyNumber) {
        this("ALREADY_ENROLLED",
                "Guest already has a loyalty account: " + existingLoyaltyNumber,
                existingLoyaltyNumber);
    }
}
