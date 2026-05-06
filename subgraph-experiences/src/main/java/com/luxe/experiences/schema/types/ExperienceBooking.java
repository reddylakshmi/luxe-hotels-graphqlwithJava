package com.luxe.experiences.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ExperienceBooking implements HasId {
    private final String id, experienceId, guestId, hotelId, confirmationCode;
    private final LocalDate date;
    private final String startTime, endTime;
    private final int participants;
    private final Money totalPrice;
    private String status, specialRequests, cancellationReason;
    private final OffsetDateTime bookedAt;
    private OffsetDateTime cancelledAt;

    public ExperienceBooking(String id, String experienceId, String guestId, String hotelId,
                              String status, LocalDate date, String startTime, String endTime,
                              int participants, Money totalPrice, String specialRequests,
                              String confirmationCode, OffsetDateTime bookedAt) {
        this.id = id; this.experienceId = experienceId; this.guestId = guestId;
        this.hotelId = hotelId; this.status = status; this.date = date;
        this.startTime = startTime; this.endTime = endTime;
        this.participants = participants; this.totalPrice = totalPrice;
        this.specialRequests = specialRequests; this.confirmationCode = confirmationCode;
        this.bookedAt = bookedAt;
    }

    @Override public String getId() { return id; }
    public String getExperienceId() { return experienceId; }
    public String getGuestId() { return guestId; }
    public String getHotelId() { return hotelId; }
    public String getStatus() { return status; }
    public LocalDate getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public int getParticipants() { return participants; }
    public Money getTotalPrice() { return totalPrice; }
    public String getSpecialRequests() { return specialRequests; }
    public String getConfirmationCode() { return confirmationCode; }
    public OffsetDateTime getBookedAt() { return bookedAt; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }

    public void cancel(String reason) {
        this.status = "CANCELLED";
        this.cancelledAt = OffsetDateTime.now();
        this.cancellationReason = reason;
    }
}
