package com.luxe.reservations.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.OffsetDateTime;

public class TransportationBooking implements HasId {
    private final String id, reservationId, type, pickupLocation, dropoffLocation;
    private final OffsetDateTime scheduledAt;
    private final String vehicleType, notes, confirmationNumber;
    private final Money price;
    private String status;
    private final OffsetDateTime createdAt;

    public TransportationBooking(String id, String reservationId, String type,
                                  String pickupLocation, String dropoffLocation,
                                  OffsetDateTime scheduledAt, String vehicleType, Money price,
                                  String status, String notes, String confirmationNumber,
                                  OffsetDateTime createdAt) {
        this.id = id; this.reservationId = reservationId; this.type = type;
        this.pickupLocation = pickupLocation; this.dropoffLocation = dropoffLocation;
        this.scheduledAt = scheduledAt; this.vehicleType = vehicleType; this.price = price;
        this.status = status; this.notes = notes; this.confirmationNumber = confirmationNumber;
        this.createdAt = createdAt;
    }

    @Override public String getId() { return id; }
    public String getReservationId() { return reservationId; }
    public String getType() { return type; }
    public String getPickupLocation() { return pickupLocation; }
    public String getDropoffLocation() { return dropoffLocation; }
    public OffsetDateTime getScheduledAt() { return scheduledAt; }
    public String getVehicleType() { return vehicleType; }
    public Money getPrice() { return price; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getConfirmationNumber() { return confirmationNumber; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setStatus(String s) { this.status = s; }
}
