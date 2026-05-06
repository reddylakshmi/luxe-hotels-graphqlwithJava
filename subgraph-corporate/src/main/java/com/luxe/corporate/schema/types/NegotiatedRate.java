package com.luxe.corporate.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record NegotiatedRate(
        String id, String accountId, String hotelId,
        String rateName, String rateCode, double discountPercent,
        Money negotiatedRate, LocalDate validFrom, LocalDate validTo,
        List<LocalDate> blackoutDates, boolean active
) implements HasId {
    @Override public String getId() { return id; }
    public Map<String, Object> getHotel() { return Map.of("id", hotelId); }
}
