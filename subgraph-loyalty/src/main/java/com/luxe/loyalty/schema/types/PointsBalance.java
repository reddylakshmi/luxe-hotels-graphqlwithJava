package com.luxe.loyalty.schema.types;

import com.luxe.common.scalar.Money;
import java.time.OffsetDateTime;

public record PointsBalance(
        int available, int pending, int expiringSoon, int total,
        Money cashEquivalent, OffsetDateTime updatedAt
) {}
