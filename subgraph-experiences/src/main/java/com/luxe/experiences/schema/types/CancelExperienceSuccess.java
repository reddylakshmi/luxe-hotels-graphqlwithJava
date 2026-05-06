package com.luxe.experiences.schema.types;

import com.luxe.common.scalar.Money;

import java.time.OffsetDateTime;

public record CancelExperienceSuccess(
        String bookingId,
        Money refundAmount,
        OffsetDateTime cancelledAt,
        String message
) {}
