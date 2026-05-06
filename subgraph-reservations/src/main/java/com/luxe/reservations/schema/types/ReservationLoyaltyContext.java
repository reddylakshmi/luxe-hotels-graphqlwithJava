package com.luxe.reservations.schema.types;

public record ReservationLoyaltyContext(
        String memberNumber, String tier, int pointsToEarn,
        Integer pointsEarned, Double bonusMultiplier, int qualifyingNights
) {}
