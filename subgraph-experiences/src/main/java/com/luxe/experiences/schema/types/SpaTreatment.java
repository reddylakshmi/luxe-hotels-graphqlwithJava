package com.luxe.experiences.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.util.List;

public record SpaTreatment(
        String id, String hotelId, String name, String category, String description,
        int durationMinutes, Money pricePerPerson,
        List<String> modalities, List<String> contraindications, Integer ageMinimum
) implements HasId {
    @Override public String getId() { return id; }
}
