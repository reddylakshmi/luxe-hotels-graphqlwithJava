package com.luxe.reservations.schema.types;

import com.luxe.common.scalar.Money;

public record ReservationCancellationPolicy(
        String type, String description,
        Integer deadlineHours, Money penaltyAmount, Float penaltyPercent
) {}
