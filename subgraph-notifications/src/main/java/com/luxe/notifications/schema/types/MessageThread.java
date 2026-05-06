package com.luxe.notifications.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageThread implements HasId {
    private final String id, guestId, hotelId, reservationId;
    private String subject;
    private String status;
    private String assignedAgent;
    private final List<Message> messages = new ArrayList<>();
    private OffsetDateTime lastMessageAt;

    public MessageThread(String id, String guestId, String subject, String status,
                          String hotelId, String reservationId, String assignedAgent,
                          OffsetDateTime lastMessageAt) {
        this.id = id; this.guestId = guestId; this.subject = subject;
        this.status = status; this.hotelId = hotelId; this.reservationId = reservationId;
        this.assignedAgent = assignedAgent; this.lastMessageAt = lastMessageAt;
    }

    @Override public String getId() { return id; }
    public String getGuestId() { return guestId; }
    public String getSubject() { return subject; }
    public String getStatus() { return status; }
    public String getHotelId() { return hotelId; }
    public String getReservationId() { return reservationId; }
    public String getAssignedAgent() { return assignedAgent; }
    public List<Message> getMessages() { return messages; }
    public OffsetDateTime getLastMessageAt() { return lastMessageAt; }
    public int getUnreadCount() {
        return (int) messages.stream()
                .filter(m -> m.getReadAt() == null && "AGENT".equals(m.getSender()))
                .count();
    }

    public void addMessage(Message m) {
        this.messages.add(m);
        this.lastMessageAt = m.getSentAt();
    }
    public void setStatus(String s) { this.status = s; }
    public void setAssignedAgent(String agent) { this.assignedAgent = agent; }
}
