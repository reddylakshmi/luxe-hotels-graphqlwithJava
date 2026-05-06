package com.luxe.loyalty.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class Certificate implements HasId {
    private final String id, type, name, description;
    private String status;
    private final OffsetDateTime issuedAt, expiresAt;
    private OffsetDateTime redeemedAt;
    private String reservationId;
    private final List<LocalDate> blackoutDates;
    private final List<String> restrictions;

    public Certificate(String id, String type, String name, String description, String status,
                       OffsetDateTime issuedAt, OffsetDateTime expiresAt,
                       OffsetDateTime redeemedAt, String reservationId,
                       List<LocalDate> blackoutDates, List<String> restrictions) {
        this.id = id; this.type = type; this.name = name; this.description = description;
        this.status = status; this.issuedAt = issuedAt; this.expiresAt = expiresAt;
        this.redeemedAt = redeemedAt; this.reservationId = reservationId;
        this.blackoutDates = blackoutDates != null ? blackoutDates : List.of();
        this.restrictions  = restrictions  != null ? restrictions  : List.of();
    }

    @Override public String getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getRedeemedAt() { return redeemedAt; }
    public String getReservationId() { return reservationId; }
    public List<LocalDate> getBlackoutDates() { return blackoutDates; }
    public List<String> getRestrictions() { return restrictions; }

    public void redeem(String reservationId) {
        this.status = "REDEEMED";
        this.redeemedAt = OffsetDateTime.now();
        this.reservationId = reservationId;
    }
}
