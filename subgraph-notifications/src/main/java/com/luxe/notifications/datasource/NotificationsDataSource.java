package com.luxe.notifications.datasource;

import com.luxe.notifications.schema.types.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface NotificationsDataSource {
    List<Notification> findByGuestId(String guestId, Map<String, Object> filter);
    Optional<Notification> findById(String id);
    int countUnread(String guestId);
    Optional<Notification> markRead(String id);
    int markAllRead(String guestId);
    Notification send(String guestId, String type, String channel, String priority,
                       String category, String title, String body,
                       String actionLabel, String actionUrl, String reservationId,
                       Map<String, String> metadata);

    NotificationPreferences findOrCreatePreferences(String guestId);
    NotificationPreferences updatePreferences(String guestId, Map<String, Object> input);

    PushDevice registerDevice(String guestId, String platform, String pushToken,
                                String appVersion, String deviceModel);
    boolean removeDevice(String deviceId);
    List<PushDevice> findDevices(String guestId);

    List<MessageThread> findThreads(String guestId, String status);
    Optional<MessageThread> findThread(String threadId);
    Message sendMessage(String guestId, Map<String, Object> input);
    MessageThread markThreadResolved(String threadId);
}
