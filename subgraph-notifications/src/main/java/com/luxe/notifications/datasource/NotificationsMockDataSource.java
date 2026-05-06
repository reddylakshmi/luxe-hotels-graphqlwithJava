package com.luxe.notifications.datasource;

import com.luxe.notifications.schema.types.*;
import com.luxe.notifications.schema.types.NotificationPreferences.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class NotificationsMockDataSource implements NotificationsDataSource {

    private final Map<String, Notification> notifications = new LinkedHashMap<>();
    private final Map<String, NotificationPreferences> preferences = new LinkedHashMap<>();
    private final Map<String, PushDevice> devices = new LinkedHashMap<>();
    private final Map<String, MessageThread> threads = new LinkedHashMap<>();

    public NotificationsMockDataSource() {
        initNotifications();
        initPreferences();
        initDevices();
        initThreads();
    }

    private void initNotifications() {
        OffsetDateTime now = OffsetDateTime.now();
        addNotification("ntf-001", "guest-001", "RESERVATION_CONFIRMED", "EMAIL",
                "NORMAL", "DELIVERED", "RESERVATIONS",
                "Your Paris reservation is confirmed",
                "We look forward to welcoming you to Le Grand Luxe Paris on May 16.",
                "View Reservation", "https://luxehotels.example/r/res-001",
                null, "res-001",
                Map.of("hotelId", "prop-paris-001"),
                now.minusDays(28), null, now.plusDays(60));
        addNotification("ntf-002", "guest-001", "CHECK_IN_REMINDER", "PUSH",
                "NORMAL", "READ", "PRE_STAY",
                "Online check-in is open",
                "You can complete check-in for your Paris stay starting tomorrow.",
                "Check-in", "https://luxehotels.example/r/res-001/checkin",
                null, "res-001",
                Map.of("hotelId", "prop-paris-001"),
                now.minusDays(2), now.minusHours(20), now.plusDays(2));
        addNotification("ntf-003", "guest-002", "LOYALTY_TIER_UPGRADE", "EMAIL",
                "HIGH", "DELIVERED", "LOYALTY",
                "Welcome to Platinum",
                "Congratulations — you've earned Platinum status. Enjoy suite upgrades and lounge access.",
                "View Benefits", "https://luxehotels.example/loyalty/platinum",
                null, null,
                Map.of("newTier", "PLATINUM"),
                now.minusDays(1), null, now.plusDays(30));
        addNotification("ntf-004", "guest-004", "FEEDBACK_REQUEST", "EMAIL",
                "LOW", "DELIVERED", "POST_STAY",
                "How was your stay?",
                "Thank you for choosing The Manhattan Pinnacle. Your feedback helps us improve.",
                "Share Feedback", "https://luxehotels.example/feedback/res-005",
                null, "res-005",
                Map.of(),
                now.minusDays(5), null, now.plusDays(14));
        addNotification("ntf-005", "guest-001", "PROMOTION_OFFER", "PUSH",
                "LOW", "PENDING", "MARKETING",
                "Spring Suite Awakening — 25% off",
                "Treat yourself to a long European weekend with 25% off all suites.",
                "View Offer", "https://luxehotels.example/offers/spring-suites",
                null, null,
                Map.of("promoCode", "SPRING25"),
                now.minusHours(4), null, now.plusDays(50));
    }

    private void addNotification(String id, String guestId, String type, String channel,
                                   String priority, String status, String category,
                                   String title, String body, String actionLabel, String actionUrl,
                                   String imageUrl, String reservationId,
                                   Map<String, String> metadata,
                                   OffsetDateTime sentAt, OffsetDateTime readAt,
                                   OffsetDateTime expiresAt) {
        notifications.put(id, new Notification(id, guestId, type, channel, priority,
                status, category, title, body,
                actionLabel, actionUrl, imageUrl, reservationId,
                metadata, sentAt, readAt, expiresAt));
    }

    private void initPreferences() {
        ChannelSetting email = new ChannelSetting("EMAIL", true, null, null);
        ChannelSetting sms   = new ChannelSetting("SMS", true,
                new QuietHours("22:00", "08:00", "Europe/Paris"), null);
        ChannelSetting push  = new ChannelSetting("PUSH", true,
                new QuietHours("23:00", "07:30", "Europe/Paris"), null);
        ChannelSetting inApp = new ChannelSetting("IN_APP", true, null, null);
        ChannelSetting wapp  = new ChannelSetting("WHATSAPP", false, null, null);

        List<CategoryPreference> cats = List.of(
                new CategoryPreference("RESERVATIONS",  List.of("EMAIL", "PUSH", "IN_APP")),
                new CategoryPreference("LOYALTY",        List.of("EMAIL", "IN_APP")),
                new CategoryPreference("MARKETING",      List.of("EMAIL")),
                new CategoryPreference("PRE_STAY",       List.of("EMAIL", "PUSH")),
                new CategoryPreference("ON_PROPERTY",    List.of("PUSH", "IN_APP")),
                new CategoryPreference("POST_STAY",      List.of("EMAIL")),
                new CategoryPreference("CONCIERGE",      List.of("PUSH", "IN_APP", "WHATSAPP")));
        preferences.put("guest-001", new NotificationPreferences(
                "pref-guest-001", "guest-001",
                List.of(email, sms, push, inApp, wapp), cats, "en", "Europe/Paris"));
        preferences.put("guest-002", new NotificationPreferences(
                "pref-guest-002", "guest-002",
                List.of(email, sms, push, inApp, wapp), cats, "en", "Europe/London"));
        preferences.put("guest-004", new NotificationPreferences(
                "pref-guest-004", "guest-004",
                List.of(email, sms, push, inApp, wapp), cats, "en", "America/New_York"));
    }

    private void initDevices() {
        OffsetDateTime now = OffsetDateTime.now();
        devices.put("dev-001", new PushDevice("dev-001", "guest-001", "IOS",
                "12.4.0", "iPhone 15 Pro", "ios-token-ABC123",
                now.minusDays(180), now.minusHours(3), true));
        devices.put("dev-002", new PushDevice("dev-002", "guest-001", "WEB",
                "1.0.0", "Chrome 134 / macOS", "web-token-XYZ789",
                now.minusDays(45), now.minusDays(2), true));
        devices.put("dev-003", new PushDevice("dev-003", "guest-002", "ANDROID",
                "12.3.0", "Pixel 9", "android-token-PQR456",
                now.minusDays(90), now.minusHours(8), true));
    }

    private void initThreads() {
        OffsetDateTime now = OffsetDateTime.now();
        MessageThread t1 = new MessageThread("thr-001", "guest-001",
                "Anniversary celebration request",
                "PENDING_AGENT", "prop-tokyo-001", "res-003", "Hiroshi T.",
                now.minusHours(6));
        t1.addMessage(new Message("msg-001", "thr-001", "GUEST",
                "Hi — celebrating our 10th anniversary in Tokyo. Could you arrange a surprise?",
                List.of(), now.minusHours(8), now.minusHours(7)));
        t1.addMessage(new Message("msg-002", "thr-001", "AGENT",
                "Wonderful — I have a few special touches in mind. May I confirm your arrival flight?",
                List.of(), now.minusHours(6), null));
        threads.put(t1.getId(), t1);

        MessageThread t2 = new MessageThread("thr-002", "guest-002",
                "Late-night dining options",
                "RESOLVED", "prop-london-001", "res-002", "Eleanor M.",
                now.minusDays(1));
        t2.addMessage(new Message("msg-003", "thr-002", "GUEST",
                "What's open after 23:00 inside the hotel?",
                List.of(), now.minusDays(1).minusMinutes(30), now.minusDays(1).minusMinutes(20)));
        t2.addMessage(new Message("msg-004", "thr-002", "AGENT",
                "Mayfair House Bar serves until 01:00; in-room dining is 24/7.",
                List.of(), now.minusDays(1), now.minusDays(1).plusMinutes(15)));
        threads.put(t2.getId(), t2);
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    @Override
    public List<Notification> findByGuestId(String guestId, Map<String, Object> filter) {
        String type = filter == null ? null : (String) filter.get("type");
        String category = filter == null ? null : (String) filter.get("category");
        String status = filter == null ? null : (String) filter.get("status");
        Boolean unreadOnly = filter == null ? null : (Boolean) filter.get("unreadOnly");
        return notifications.values().stream()
                .filter(n -> n.getGuestId().equals(guestId))
                .filter(n -> type == null || n.getType().equals(type))
                .filter(n -> category == null || n.getCategory().equals(category))
                .filter(n -> status == null || n.getStatus().equals(status))
                .filter(n -> unreadOnly == null || !unreadOnly || !"READ".equals(n.getStatus()))
                .sorted(Comparator.comparing(Notification::getSentAt).reversed())
                .collect(Collectors.toList());
    }

    @Override public Optional<Notification> findById(String id) {
        return Optional.ofNullable(notifications.get(id));
    }

    @Override
    public int countUnread(String guestId) {
        return (int) notifications.values().stream()
                .filter(n -> n.getGuestId().equals(guestId))
                .filter(n -> !"READ".equals(n.getStatus()))
                .count();
    }

    @Override
    public Optional<Notification> markRead(String id) {
        Notification n = notifications.get(id);
        if (n == null) return Optional.empty();
        n.markRead();
        return Optional.of(n);
    }

    @Override
    public int markAllRead(String guestId) {
        int count = 0;
        for (Notification n : notifications.values()) {
            if (n.getGuestId().equals(guestId) && !"READ".equals(n.getStatus())) {
                n.markRead();
                count++;
            }
        }
        return count;
    }

    @Override
    public Notification send(String guestId, String type, String channel, String priority,
                              String category, String title, String body,
                              String actionLabel, String actionUrl, String reservationId,
                              Map<String, String> metadata) {
        String id = "ntf-" + UUID.randomUUID().toString().substring(0, 8);
        Notification n = new Notification(id, guestId, type, channel,
                priority != null ? priority : "NORMAL",
                "DELIVERED", category, title, body, actionLabel, actionUrl,
                null, reservationId, metadata, OffsetDateTime.now(), null,
                OffsetDateTime.now().plusDays(30));
        notifications.put(id, n);
        return n;
    }

    // ── Preferences ───────────────────────────────────────────────────────────

    @Override
    public NotificationPreferences findOrCreatePreferences(String guestId) {
        return preferences.computeIfAbsent(guestId, gid -> {
            ChannelSetting email = new ChannelSetting("EMAIL", true, null, null);
            ChannelSetting push  = new ChannelSetting("PUSH",  true, null, null);
            ChannelSetting inApp = new ChannelSetting("IN_APP", true, null, null);
            return new NotificationPreferences(
                    "pref-" + gid, gid,
                    List.of(email, push, inApp),
                    List.of(new CategoryPreference("RESERVATIONS", List.of("EMAIL", "PUSH"))),
                    "en", "UTC");
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public NotificationPreferences updatePreferences(String guestId, Map<String, Object> input) {
        NotificationPreferences p = findOrCreatePreferences(guestId);
        List<Map<String, Object>> channelsRaw =
                (List<Map<String, Object>>) input.get("channelSettings");
        List<ChannelSetting> channels = channelsRaw == null ? null
                : channelsRaw.stream().map(m -> {
                    QuietHours qh = m.get("quietHoursStart") != null
                            ? new QuietHours((String) m.get("quietHoursStart"),
                                    (String) m.get("quietHoursEnd"),
                                    (String) m.getOrDefault("quietHoursTimezone", "UTC"))
                            : null;
                    return new ChannelSetting((String) m.get("channel"),
                            Boolean.TRUE.equals(m.get("enabled")),
                            qh,
                            (String) m.get("preferredAddress"));
                }).toList();
        List<Map<String, Object>> catsRaw =
                (List<Map<String, Object>>) input.get("categoryPreferences");
        List<CategoryPreference> cats = catsRaw == null ? null
                : catsRaw.stream().map(m -> new CategoryPreference(
                        (String) m.get("category"),
                        (List<String>) m.get("enabledChannels"))).toList();
        p.apply((String) input.get("language"), (String) input.get("timezone"), channels, cats);
        return p;
    }

    // ── Devices ───────────────────────────────────────────────────────────────

    @Override
    public PushDevice registerDevice(String guestId, String platform, String pushToken,
                                       String appVersion, String deviceModel) {
        String id = "dev-" + UUID.randomUUID().toString().substring(0, 8);
        PushDevice d = new PushDevice(id, guestId, platform, appVersion, deviceModel,
                pushToken, OffsetDateTime.now(), OffsetDateTime.now(), true);
        devices.put(id, d);
        return d;
    }

    @Override public boolean removeDevice(String deviceId) {
        PushDevice d = devices.get(deviceId);
        if (d == null) return false;
        d.deactivate();
        return true;
    }

    @Override
    public List<PushDevice> findDevices(String guestId) {
        return devices.values().stream()
                .filter(d -> d.getGuestId().equals(guestId) && d.isActive())
                .collect(Collectors.toList());
    }

    // ── Message threads ───────────────────────────────────────────────────────

    @Override
    public List<MessageThread> findThreads(String guestId, String status) {
        return threads.values().stream()
                .filter(t -> t.getGuestId().equals(guestId))
                .filter(t -> status == null || t.getStatus().equals(status))
                .sorted(Comparator.comparing(MessageThread::getLastMessageAt).reversed())
                .collect(Collectors.toList());
    }

    @Override public Optional<MessageThread> findThread(String id) {
        return Optional.ofNullable(threads.get(id));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Message sendMessage(String guestId, Map<String, Object> input) {
        String threadId = (String) input.get("threadId");
        MessageThread thread;
        if (threadId != null) {
            thread = threads.get(threadId);
            if (thread == null) return null;
        } else {
            String newId = "thr-" + UUID.randomUUID().toString().substring(0, 8);
            String subject = (String) input.getOrDefault("subject", "New conversation");
            thread = new MessageThread(newId, guestId, subject, "PENDING_AGENT",
                    (String) input.get("hotelId"),
                    (String) input.get("reservationId"),
                    null, OffsetDateTime.now());
            threads.put(newId, thread);
        }
        String mid = "msg-" + UUID.randomUUID().toString().substring(0, 8);
        List<String> attachments = (List<String>) input.get("attachmentUrls");
        Message m = new Message(mid, thread.getId(), "GUEST",
                (String) input.get("body"), attachments, OffsetDateTime.now(), null);
        thread.addMessage(m);
        if ("RESOLVED".equals(thread.getStatus()) || "CLOSED".equals(thread.getStatus())) {
            thread.setStatus("PENDING_AGENT");
        }
        return m;
    }

    @Override
    public MessageThread markThreadResolved(String threadId) {
        MessageThread t = threads.get(threadId);
        if (t == null) return null;
        t.setStatus("RESOLVED");
        return t;
    }
}
