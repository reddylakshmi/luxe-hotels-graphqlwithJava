package com.luxe.pricing.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.LocalDate;
import java.util.List;

public class Promotion implements HasId {
    private final String id, code, name, description, discountType, brandId;
    private final double discountValue;
    private final Integer minimumStay, maxUsesPerGuest;
    private final List<String> hotelIds;
    private final boolean memberOnly, stackable, active;
    private final LocalDate validFrom, validTo;

    public Promotion(String id, String code, String name, String description,
                     String discountType, double discountValue, Integer minimumStay,
                     String brandId, List<String> hotelIds,
                     boolean memberOnly, boolean stackable, Integer maxUsesPerGuest,
                     LocalDate validFrom, LocalDate validTo, boolean active) {
        this.id = id; this.code = code; this.name = name; this.description = description;
        this.discountType = discountType; this.discountValue = discountValue;
        this.minimumStay = minimumStay; this.brandId = brandId; this.hotelIds = hotelIds;
        this.memberOnly = memberOnly; this.stackable = stackable;
        this.maxUsesPerGuest = maxUsesPerGuest;
        this.validFrom = validFrom; this.validTo = validTo; this.active = active;
    }

    @Override public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getDiscountType() { return discountType; }
    public double getDiscountValue() { return discountValue; }
    public Integer getMinimumStay() { return minimumStay; }
    public String getBrandId() { return brandId; }
    public List<String> getHotelIds() { return hotelIds != null ? hotelIds : List.of(); }
    public boolean isMemberOnly() { return memberOnly; }
    public boolean isStackable() { return stackable; }
    public Integer getMaxUsesPerGuest() { return maxUsesPerGuest; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public boolean isActive() { return active; }
}
