package com.luxe.loyalty.schema.types;

import com.luxe.common.scalar.Money;
import java.util.List;

public record PointsValuation(
        int points, String currency, Money cashValue, Money ratePerThousand,
        String bestUse, List<PointsRedemptionExample> comparisonRedemptions
) {
    public record PointsRedemptionExample(String name, int points, Money approxValue) {}
}
