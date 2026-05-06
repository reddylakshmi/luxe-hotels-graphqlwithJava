package com.luxe.pricing.schema.types;

import com.luxe.common.scalar.Money;
import java.time.LocalDate;
import java.util.List;

public record RedemptionRate(
        String roomTypeId, String hotelId,
        int pointsCost, int pointsPerNight,
        List<LocalDate> availableNights, Money cashCopay
) {}
