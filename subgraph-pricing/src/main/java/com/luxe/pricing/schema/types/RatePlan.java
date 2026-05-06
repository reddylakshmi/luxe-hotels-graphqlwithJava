package com.luxe.pricing.schema.types;

import com.luxe.common.pagination.HasId;

public class RatePlan implements HasId {
    private final String id, code, name, type, description;
    private final boolean refundable, breakfastIncluded, wifiIncluded, parkingIncluded, loyaltyEligible;
    private final Double loyaltyMultiplier;
    private final CancellationPolicy cancellationPolicy;
    private final Integer minNights, maxNights, advanceBookingDays;

    public RatePlan(String id, String code, String name, String type, String description,
                    boolean refundable, boolean breakfastIncluded, boolean wifiIncluded,
                    boolean parkingIncluded, boolean loyaltyEligible, Double loyaltyMultiplier,
                    CancellationPolicy cancellationPolicy,
                    Integer minNights, Integer maxNights, Integer advanceBookingDays) {
        this.id = id; this.code = code; this.name = name; this.type = type;
        this.description = description; this.refundable = refundable;
        this.breakfastIncluded = breakfastIncluded; this.wifiIncluded = wifiIncluded;
        this.parkingIncluded = parkingIncluded; this.loyaltyEligible = loyaltyEligible;
        this.loyaltyMultiplier = loyaltyMultiplier; this.cancellationPolicy = cancellationPolicy;
        this.minNights = minNights; this.maxNights = maxNights;
        this.advanceBookingDays = advanceBookingDays;
    }

    @Override public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public boolean isRefundable() { return refundable; }
    public boolean isBreakfastIncluded() { return breakfastIncluded; }
    public boolean isWifiIncluded() { return wifiIncluded; }
    public boolean isParkingIncluded() { return parkingIncluded; }
    public boolean isLoyaltyEligible() { return loyaltyEligible; }
    public Double getLoyaltyMultiplier() { return loyaltyMultiplier; }
    public CancellationPolicy getCancellationPolicy() { return cancellationPolicy; }
    public Integer getMinNights() { return minNights; }
    public Integer getMaxNights() { return maxNights; }
    public Integer getAdvanceBookingDays() { return advanceBookingDays; }
}
