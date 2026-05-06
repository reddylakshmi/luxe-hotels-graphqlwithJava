package com.luxe.reservations.schema.types;

import com.luxe.common.scalar.Money;
import java.time.OffsetDateTime;

public class RoomUpgradeRequest {
    private final String id, requestedRoomTypeId, requestedRoomTypeName, reason;
    private String status;
    private final Money upgradePrice;
    private OffsetDateTime confirmedAt;

    public RoomUpgradeRequest(String id, String requestedRoomTypeId, String requestedRoomTypeName,
                               String status, Money upgradePrice, OffsetDateTime confirmedAt,
                               String reason) {
        this.id = id; this.requestedRoomTypeId = requestedRoomTypeId;
        this.requestedRoomTypeName = requestedRoomTypeName; this.status = status;
        this.upgradePrice = upgradePrice; this.confirmedAt = confirmedAt; this.reason = reason;
    }

    public String getId() { return id; }
    public String getRequestedRoomTypeId() { return requestedRoomTypeId; }
    public String getRequestedRoomTypeName() { return requestedRoomTypeName; }
    public String getStatus() { return status; }
    public Money getUpgradePrice() { return upgradePrice; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public String getReason() { return reason; }
    public void setStatus(String s) { this.status = s; }
}
