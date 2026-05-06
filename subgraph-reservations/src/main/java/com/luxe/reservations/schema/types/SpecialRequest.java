package com.luxe.reservations.schema.types;

import java.time.OffsetDateTime;

public record SpecialRequest(
        String id, String category, String request,
        String status, OffsetDateTime fulfilledAt
) {}
