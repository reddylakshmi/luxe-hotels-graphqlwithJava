package com.luxe.property.schema.types;

import com.luxe.common.pagination.HasId;
import java.util.List;

public record Restaurant(
        String id, String hotelId, String name, List<String> cuisine,
        LocalizedContent description, String dressCode, String openingHours,
        String priceRange, boolean michelin, boolean reservationRequired
) implements HasId {
    @Override public String getId() { return id; }
}
