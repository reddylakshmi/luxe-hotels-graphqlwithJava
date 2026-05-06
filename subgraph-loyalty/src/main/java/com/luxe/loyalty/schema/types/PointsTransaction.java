package com.luxe.loyalty.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.OffsetDateTime;

public record PointsTransaction(
        String id, String accountId, String type, int points, int balanceAfter,
        String description, String reservationId, String partnerId,
        OffsetDateTime transactionDate, OffsetDateTime expiresAt
) implements HasId {
    @Override public String getId() { return id; }
}
