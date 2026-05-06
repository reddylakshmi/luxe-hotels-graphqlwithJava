package com.luxe.loyalty.schema.types;

public record AirlineTransferSuccess(
        String transactionId,
        int pointsDeducted,
        int partnerMilesAwarded,
        LoyaltyPartner partner,
        int estimatedDeliveryDays,
        String message
) {}
