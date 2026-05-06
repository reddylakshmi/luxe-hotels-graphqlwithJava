package com.luxe.reservations.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class SpaAppointment implements HasId {
    private final String id, reservationId, treatmentId, treatmentName;
    private final String therapistName;
    private final LocalDate date;
    private final String time;
    private final int durationMinutes;
    private final Money price;
    private String status;
    private final String confirmationNumber;
    private final OffsetDateTime createdAt;

    public SpaAppointment(String id, String reservationId, String treatmentId,
                           String treatmentName, String therapistName,
                           LocalDate date, String time, int durationMinutes, Money price,
                           String status, String confirmationNumber, OffsetDateTime createdAt) {
        this.id = id; this.reservationId = reservationId; this.treatmentId = treatmentId;
        this.treatmentName = treatmentName; this.therapistName = therapistName;
        this.date = date; this.time = time; this.durationMinutes = durationMinutes;
        this.price = price; this.status = status; this.confirmationNumber = confirmationNumber;
        this.createdAt = createdAt;
    }

    @Override public String getId() { return id; }
    public String getReservationId() { return reservationId; }
    public String getTreatmentId() { return treatmentId; }
    public String getTreatmentName() { return treatmentName; }
    public String getTherapistName() { return therapistName; }
    public LocalDate getDate() { return date; }
    public String getTime() { return time; }
    public int getDurationMinutes() { return durationMinutes; }
    public Money getPrice() { return price; }
    public String getStatus() { return status; }
    public String getConfirmationNumber() { return confirmationNumber; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setStatus(String s) { this.status = s; }
}
