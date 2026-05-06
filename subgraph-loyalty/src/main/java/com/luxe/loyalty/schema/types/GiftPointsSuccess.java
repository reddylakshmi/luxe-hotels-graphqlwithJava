package com.luxe.loyalty.schema.types;

public record GiftPointsSuccess(
        String transactionId,
        String recipientLoyaltyNumber,
        int pointsGifted,
        int newBalance,
        String message
) {}
