package com.luxe.pricing.schema.types;

import java.util.List;
import java.util.Map;

public class RoomAvailability {
    private final Map<String, Object> roomType;
    private final List<Rate> rates;
    private final Rate lowestRate;
    private final int availableCount;
    private final UrgencySignal urgencySignal;

    public RoomAvailability(String roomTypeId, List<Rate> rates, Rate lowestRate,
                             int availableCount, UrgencySignal urgencySignal) {
        this.roomType = Map.of("id", roomTypeId);
        this.rates = rates;
        this.lowestRate = lowestRate;
        this.availableCount = availableCount;
        this.urgencySignal = urgencySignal;
    }

    public Map<String, Object> getRoomType() { return roomType; }
    public List<Rate> getRates() { return rates; }
    public Rate getLowestRate() { return lowestRate; }
    public int getAvailableCount() { return availableCount; }
    public UrgencySignal getUrgencySignal() { return urgencySignal; }
}
