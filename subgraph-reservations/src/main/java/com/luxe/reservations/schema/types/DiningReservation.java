package com.luxe.reservations.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class DiningReservation implements HasId {
    private final String id, reservationId, restaurantId, restaurantName;
    private final LocalDate date;
    private final String time;
    private final int partySize;
    private String status;
    private final String specialRequests, confirmationNumber;
    private final OffsetDateTime createdAt;

    public DiningReservation(String id, String reservationId, String restaurantId,
                              String restaurantName, LocalDate date, String time,
                              int partySize, String status, String specialRequests,
                              String confirmationNumber, OffsetDateTime createdAt) {
        this.id = id; this.reservationId = reservationId; this.restaurantId = restaurantId;
        this.restaurantName = restaurantName; this.date = date; this.time = time;
        this.partySize = partySize; this.status = status; this.specialRequests = specialRequests;
        this.confirmationNumber = confirmationNumber; this.createdAt = createdAt;
    }

    @Override public String getId() { return id; }
    public String getReservationId() { return reservationId; }
    public String getRestaurantId() { return restaurantId; }
    public String getRestaurantName() { return restaurantName; }
    public LocalDate getDate() { return date; }
    public String getTime() { return time; }
    public int getPartySize() { return partySize; }
    public String getStatus() { return status; }
    public String getSpecialRequests() { return specialRequests; }
    public String getConfirmationNumber() { return confirmationNumber; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setStatus(String s) { this.status = s; }
}
