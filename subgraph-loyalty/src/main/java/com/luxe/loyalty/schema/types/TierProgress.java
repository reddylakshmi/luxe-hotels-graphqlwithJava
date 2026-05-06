package com.luxe.loyalty.schema.types;

import com.luxe.common.scalar.Money;
import java.time.LocalDate;

public record TierProgress(
        String currentTier, String nextTier,
        int qualifyingNights, Integer nightsToNextTier, Integer nightsToRetain,
        Money qualifyingSpend, double tierProgressPct,
        LocalDate qualificationYearEndDate, String projectedTier
) {}
