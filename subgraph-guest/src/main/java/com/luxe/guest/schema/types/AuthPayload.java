package com.luxe.guest.schema.types;

public record AuthPayload(
        String accessToken, String refreshToken,
        int expiresIn, String tokenType,
        GuestProfile guest, Boolean isNewAccount
) {}
