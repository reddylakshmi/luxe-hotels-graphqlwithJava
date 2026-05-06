package com.luxe.experiences.schema.types;

import java.time.LocalDate;
import java.util.List;

public record RestaurantAvailability(
        String hotelId, LocalDate date, int partySize, List<RestaurantSlots> restaurants
) {
    public record RestaurantSlots(
            String restaurantId, String restaurantName, String cuisine,
            Integer michelinStars, List<TimeSlot> slots
    ) {}
}
