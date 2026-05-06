package com.luxe.meetings.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.util.List;
import java.util.Map;

public record EventSpace(
        String id, String hotelId, String name, String description, String category,
        List<CapacityStyle> capacityStyles, double areaSqFt, double areaSqMeters,
        double ceilingHeightFt, boolean naturalLight, boolean blackoutCapable,
        int rooms, boolean divisible,
        TechnicalSpecs technicalSpecs, List<AVEquipment> avEquipment,
        boolean cateringRequired, EventSpaceRateCard rateCard,
        List<String> images, String floorPlanUrl
) implements HasId {
    @Override public String getId() { return id; }
    public Map<String, Object> getHotel() { return Map.of("id", hotelId); }

    public record CapacityStyle(String setup, int capacity) {}

    public record TechnicalSpecs(
            String power, int internetSpeedMbps, Integer riggingPoints,
            Double loadInDoorsHeightFt, boolean freightElevator, String noiseRating
    ) {}

    public record AVEquipment(
            String category, String name, String model,
            int quantity, boolean includedInRate, Money rentalCost
    ) {}

    public record EventSpaceRateCard(
            Money fullDay, Money halfDay, Money hourly,
            Money setupFee, Money cleaningFee, Money minimumFAndBSpend, String currency
    ) {}
}
