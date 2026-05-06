package com.luxe.reservations.schema.types;

import java.time.OffsetDateTime;
import java.util.List;

public class DigitalKey {
    private final String reservationId, keyCode;
    private String status;
    private OffsetDateTime activatedAt, expiresAt;
    private final List<String> rooms;

    public DigitalKey(String reservationId, String keyCode, String status,
                       OffsetDateTime activatedAt, OffsetDateTime expiresAt, List<String> rooms) {
        this.reservationId = reservationId; this.keyCode = keyCode; this.status = status;
        this.activatedAt = activatedAt; this.expiresAt = expiresAt; this.rooms = rooms;
    }

    public String getReservationId() { return reservationId; }
    public String getKeyCode() { return keyCode; }
    public String getStatus() { return status; }
    public OffsetDateTime getActivatedAt() { return activatedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public List<String> getRooms() { return rooms; }

    public void activate() { this.status = "ACTIVE"; this.activatedAt = OffsetDateTime.now(); }
}
