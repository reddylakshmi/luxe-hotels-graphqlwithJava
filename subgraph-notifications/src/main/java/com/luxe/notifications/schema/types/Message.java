package com.luxe.notifications.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.OffsetDateTime;
import java.util.List;

public class Message implements HasId {
    private final String id, threadId, sender, body;
    private final List<String> attachmentUrls;
    private final OffsetDateTime sentAt;
    private OffsetDateTime readAt;

    public Message(String id, String threadId, String sender, String body,
                    List<String> attachmentUrls, OffsetDateTime sentAt, OffsetDateTime readAt) {
        this.id = id; this.threadId = threadId; this.sender = sender; this.body = body;
        this.attachmentUrls = attachmentUrls != null ? attachmentUrls : List.of();
        this.sentAt = sentAt; this.readAt = readAt;
    }

    @Override public String getId() { return id; }
    public String getThreadId() { return threadId; }
    public String getSender() { return sender; }
    public String getBody() { return body; }
    public List<String> getAttachmentUrls() { return attachmentUrls; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public OffsetDateTime getReadAt() { return readAt; }
    public void markRead() { this.readAt = OffsetDateTime.now(); }
}
