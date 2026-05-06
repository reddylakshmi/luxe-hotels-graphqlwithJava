package com.luxe.property.schema.types;

import java.time.OffsetDateTime;

public record ReviewResponse(OffsetDateTime respondedAt, String body) {}
