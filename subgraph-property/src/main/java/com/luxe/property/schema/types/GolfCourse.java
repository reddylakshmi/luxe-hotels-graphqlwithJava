package com.luxe.property.schema.types;

import com.luxe.common.pagination.HasId;

public record GolfCourse(
        String id, String hotelId, String name, int holes,
        Integer par, String designer, LocalizedContent description
) implements HasId {
    @Override public String getId() { return id; }
}
