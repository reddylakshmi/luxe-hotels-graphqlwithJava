package com.luxe.pricing.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.OffsetDateTime;
import java.util.List;

public class Rate implements HasId {
    private final String id;
    private final String hotelId;
    private final String roomTypeId;
    private final RatePlan ratePlan;
    private final List<NightlyRate> nightlyRates;
    private final Money totalRate;
    private final Money totalWithTaxes;
    private final Money averageNightlyRate;
    private final TaxBreakdown taxesAndFees;
    private final Money strikethrough;
    private final Money savingsAmount;
    private final Double savingsPct;
    private final int pointsEarned;
    private final int availableRooms;
    private final String rateToken;
    private final OffsetDateTime expiresAt;

    public Rate(String id, String hotelId, String roomTypeId, RatePlan ratePlan,
                List<NightlyRate> nightlyRates, Money totalRate, Money totalWithTaxes,
                Money averageNightlyRate, TaxBreakdown taxesAndFees,
                Money strikethrough, Money savingsAmount, Double savingsPct,
                int pointsEarned, int availableRooms, String rateToken, OffsetDateTime expiresAt) {
        this.id = id; this.hotelId = hotelId; this.roomTypeId = roomTypeId;
        this.ratePlan = ratePlan; this.nightlyRates = nightlyRates;
        this.totalRate = totalRate; this.totalWithTaxes = totalWithTaxes;
        this.averageNightlyRate = averageNightlyRate; this.taxesAndFees = taxesAndFees;
        this.strikethrough = strikethrough; this.savingsAmount = savingsAmount;
        this.savingsPct = savingsPct; this.pointsEarned = pointsEarned;
        this.availableRooms = availableRooms; this.rateToken = rateToken;
        this.expiresAt = expiresAt;
    }

    @Override public String getId() { return id; }
    public String getHotelId() { return hotelId; }
    public String getRoomTypeId() { return roomTypeId; }
    public RatePlan getRatePlan() { return ratePlan; }
    public List<NightlyRate> getNightlyRates() { return nightlyRates; }
    public Money getTotalRate() { return totalRate; }
    public Money getTotalWithTaxes() { return totalWithTaxes; }
    public Money getAverageNightlyRate() { return averageNightlyRate; }
    public TaxBreakdown getTaxesAndFees() { return taxesAndFees; }
    public Money getStrikethrough() { return strikethrough; }
    public Money getSavingsAmount() { return savingsAmount; }
    public Double getSavingsPct() { return savingsPct; }
    public int getPointsEarned() { return pointsEarned; }
    public int getAvailableRooms() { return availableRooms; }
    public String getRateToken() { return rateToken; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
