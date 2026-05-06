package com.luxe.reservations.schema.types;

import com.luxe.common.scalar.Money;
import java.time.OffsetDateTime;

public record CancellationRecord(
        OffsetDateTime cancelledAt, String reason, String cancelledBy,
        Money refundAmount, String refundStatus, String cancellationNumber
) {}
