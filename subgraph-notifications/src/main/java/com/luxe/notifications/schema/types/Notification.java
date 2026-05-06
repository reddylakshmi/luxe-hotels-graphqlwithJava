package com.luxe.notifications.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class Notification implements HasId {
    private final String id, guestId, type, channel, priority, category;
    private String status;
    private final String title, body, actionLabel, actionUrl, imageUrl, reservationId;
    private final Map<String, String> metadata;
    private final OffsetDateTime sentAt;
    private OffsetDateTime readAt;
    private final OffsetDateTime expiresAt;

    public Notification(String id, String guestId, String type, String channel, String priority,
                        String status, String category, String title, String body,
                        String actionLabel, String actionUrl, String imageUrl, String reservationId,
                        Map<String, String> metadata, OffsetDateTime sentAt,
                        OffsetDateTime readAt, OffsetDateTime expiresAt) {
        this.id = id; this.guestId = guestId; this.type = type; this.channel = channel;
        this.priority = priority; this.status = status; this.category = category;
        this.title = title; this.body = body;
        this.actionLabel = actionLabel; this.actionUrl = actionUrl; this.imageUrl = imageUrl;
        this.reservationId = reservationId;
        this.metadata = metadata != null ? metadata : Map.of();
        this.sentAt = sentAt; this.readAt = readAt; this.expiresAt = expiresAt;
    }

    @Override public String getId() { return id; }
    public String getGuestId() { return guestId; }
    public String getType() { return type; }
    public String getChannel() { return channel; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getActionLabel() { return actionLabel; }
    public String getActionUrl() { return actionUrl; }
    public String getImageUrl() { return imageUrl; }
    public String getReservationId() { return reservationId; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public OffsetDateTime getReadAt() { return readAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }

    public List<Map<String, String>> getMetadata() {
        return metadata.entrySet().stream()
                .map(e -> Map.of("key", e.getKey(), "value", e.getValue()))
                .toList();
    }

    public void markRead() {
        this.status = "READ";
        this.readAt = OffsetDateTime.now();
    }
}
