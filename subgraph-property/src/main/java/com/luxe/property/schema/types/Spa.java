package com.luxe.property.schema.types;

import com.luxe.common.pagination.HasId;

public record Spa(
        String id, String hotelId, String name,
        LocalizedContent description, String openingHours, boolean bookingRequired
) implements HasId {
    @Override public String getId() { return id; }
}
