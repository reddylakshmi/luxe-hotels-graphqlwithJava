package com.luxe.experiences.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.util.List;
import java.util.Map;

public record Experience(
        String id, String hotelId, String name, String category,
        String description, String longDescription,
        Integer durationMinutes, Money pricePerPerson, String currency,
        int maxParticipants, int minParticipants, boolean requiresBooking,
        List<String> highlights, List<String> included, List<String> bringYourOwn,
        List<String> images, boolean featured, boolean available,
        String difficulty, Integer ageMinimum
) implements HasId {
    @Override public String getId() { return id; }

    public Map<String, Object> getHotel() { return Map.of("id", hotelId); }
}
