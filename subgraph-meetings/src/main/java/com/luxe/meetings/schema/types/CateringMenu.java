package com.luxe.meetings.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.util.List;

public record CateringMenu(
        String id, String hotelId, String name, String description,
        Money pricePerPerson, int minimumGuests,
        List<CateringCourse> courses, List<String> beverageOptions
) implements HasId {
    @Override public String getId() { return id; }

    public record CateringCourse(String name, String description) {}
}
