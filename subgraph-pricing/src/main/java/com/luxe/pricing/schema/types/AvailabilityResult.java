package com.luxe.pricing.schema.types;

import com.luxe.common.scalar.Money;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class AvailabilityResult {
    private final Map<String, Object> hotel;
    private final LocalDate checkIn, checkOut;
    private final int nights;
    private final GuestCount guestCount;
    private final String currency;
    private final List<RoomAvailability> roomAvailabilities;
    private final Money lowestRate;
    private final List<DateRateSummary> rateSummaryByDate;
    private final String searchToken;
    private final OffsetDateTime expiresAt;

    public AvailabilityResult(String hotelId, LocalDate checkIn, LocalDate checkOut,
                               GuestCount guestCount, String currency,
                               List<RoomAvailability> roomAvailabilities, Money lowestRate,
                               List<DateRateSummary> rateSummaryByDate,
                               String searchToken, OffsetDateTime expiresAt) {
        this.hotel = Map.of("id", hotelId);
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = (int) checkIn.until(checkOut).getDays();
        this.guestCount = guestCount;
        this.currency = currency;
        this.roomAvailabilities = roomAvailabilities;
        this.lowestRate = lowestRate;
        this.rateSummaryByDate = rateSummaryByDate;
        this.searchToken = searchToken;
        this.expiresAt = expiresAt;
    }

    public Map<String, Object> getHotel() { return hotel; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getNights() { return nights; }
    public GuestCount getGuestCount() { return guestCount; }
    public String getCurrency() { return currency; }
    public List<RoomAvailability> getRoomAvailabilities() { return roomAvailabilities; }
    public Money getLowestRate() { return lowestRate; }
    public List<DateRateSummary> getRateSummaryByDate() { return rateSummaryByDate; }
    public String getSearchToken() { return searchToken; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
