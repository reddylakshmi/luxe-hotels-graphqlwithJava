package com.luxe.notifications.datasource;

import com.luxe.notifications.schema.types.Message;
import com.luxe.notifications.schema.types.MessageThread;
import com.luxe.notifications.schema.types.Notification;
import com.luxe.notifications.schema.types.NotificationPreferences;
import com.luxe.notifications.schema.types.PushDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives branch coverage: filter combinations, send/markRead/sendMessage edge cases,
 * preference apply paths with quiet-hours and per-channel settings.
 */
class NotificationsBranchesTest {

    private NotificationsMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new NotificationsMockDataSource();
    }

    // ── findByGuestId filter branches ────────────────────────────────────────

    @Test
    void find_by_guest_id_filters_by_type() {
        var checkInOnly = ds.findByGuestId("guest-001",
                Map.of("type", "CHECK_IN_REMINDER"));
        assertThat(checkInOnly).allSatisfy(n ->
                assertThat(n.getType()).isEqualTo("CHECK_IN_REMINDER"));
    }

    @Test
    void find_by_guest_id_filters_by_category() {
        var marketing = ds.findByGuestId("guest-001",
                Map.of("category", "MARKETING"));
        assertThat(marketing).allSatisfy(n ->
                assertThat(n.getCategory()).isEqualTo("MARKETING"));
    }

    @Test
    void find_by_guest_id_filters_by_status() {
        var read = ds.findByGuestId("guest-001", Map.of("status", "READ"));
        assertThat(read).allSatisfy(n -> assertThat(n.getStatus()).isEqualTo("READ"));
    }

    @Test
    void find_by_guest_id_unread_only_excludes_read() {
        var unread = ds.findByGuestId("guest-001", Map.of("unreadOnly", true));
        assertThat(unread).allSatisfy(n -> assertThat(n.getStatus()).isNotEqualTo("READ"));
    }

    @Test
    void find_by_guest_id_unread_only_false_includes_everything() {
        var all = ds.findByGuestId("guest-001", Map.of("unreadOnly", false));
        assertThat(all).hasSize(ds.findByGuestId("guest-001", null).size());
    }

    @Test
    void find_by_unknown_guest_id_returns_empty() {
        assertThat(ds.findByGuestId("not-a-guest", null)).isEmpty();
    }

    // ── send / markRead / countUnread ────────────────────────────────────────

    @Test
    void send_creates_delivered_notification_with_default_priority() {
        Notification n = ds.send("guest-001", "WELCOME_MESSAGE", "EMAIL", null,
                "ACCOUNT", "Welcome", "Body", null, null, null, Map.of("k", "v"));
        assertThat(n).isNotNull();
        assertThat(n.getPriority()).isEqualTo("NORMAL");
        assertThat(n.getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void send_with_explicit_priority_preserves_it() {
        Notification n = ds.send("guest-001", "WELCOME_MESSAGE", "EMAIL", "URGENT",
                "ACCOUNT", "Title", "Body", null, null, null, null);
        assertThat(n.getPriority()).isEqualTo("URGENT");
    }

    @Test
    void mark_read_unknown_id_returns_empty() {
        assertThat(ds.markRead("ntf-not-real")).isEmpty();
    }

    @Test
    void mark_all_read_returns_zero_when_no_unread() {
        ds.markAllRead("guest-001");                // first call clears them
        assertThat(ds.markAllRead("guest-001")).isZero(); // second call: nothing to do
    }

    @Test
    void count_unread_decreases_after_mark_read() {
        int before = ds.countUnread("guest-001");
        if (before > 0) {
            Notification any = ds.findByGuestId("guest-001", Map.of("unreadOnly", true)).get(0);
            ds.markRead(any.getId());
            assertThat(ds.countUnread("guest-001")).isEqualTo(before - 1);
        }
    }

    // ── findThreads filter branch ────────────────────────────────────────────

    @Test
    void find_threads_filters_by_status() {
        var resolved = ds.findThreads("guest-002", "RESOLVED");
        assertThat(resolved).allSatisfy(t -> assertThat(t.getStatus()).isEqualTo("RESOLVED"));
    }

    @Test
    void find_threads_unknown_status_returns_empty() {
        assertThat(ds.findThreads("guest-001", "BOGUS")).isEmpty();
    }

    @Test
    void find_thread_by_unknown_id_is_empty() {
        assertThat(ds.findThread("not-real")).isEmpty();
    }

    // ── sendMessage thread routing ───────────────────────────────────────────

    @Test
    void send_message_with_existing_thread_id_appends_to_thread() {
        // discover an existing thread for guest-001
        MessageThread thread = ds.findThreads("guest-001", null).get(0);
        int before = thread.getMessages().size();
        Message m = ds.sendMessage("guest-001", Map.of(
                "threadId", thread.getId(), "body", "Follow-up question"));
        assertThat(m).isNotNull();
        assertThat(thread.getMessages()).hasSize(before + 1);
    }

    @Test
    void send_message_without_thread_id_creates_new_thread_pending_agent() {
        Message m = ds.sendMessage("guest-001", Map.of(
                "subject", "New question", "body", "Hello",
                "hotelId", "prop-paris-001"));
        assertThat(m).isNotNull();
        MessageThread t = ds.findThread(m.getThreadId()).orElseThrow();
        assertThat(t.getStatus()).isEqualTo("PENDING_AGENT");
        assertThat(t.getSubject()).isEqualTo("New question");
    }

    @Test
    void send_message_to_resolved_thread_reopens_it_to_pending_agent() {
        // discover the seeded RESOLVED thread for guest-002 and reopen
        MessageThread resolved = ds.findThreads("guest-002", "RESOLVED").get(0);
        ds.sendMessage("guest-002", Map.of("threadId", resolved.getId(), "body", "Reopening"));
        assertThat(ds.findThread(resolved.getId()).orElseThrow().getStatus())
                .isEqualTo("PENDING_AGENT");
    }

    @Test
    void send_message_to_unknown_thread_id_returns_null() {
        Message m = ds.sendMessage("guest-001",
                Map.of("threadId", "not-real", "body", "Hi"));
        assertThat(m).isNull();
    }

    @Test
    void send_message_with_attachments_persists_them() {
        Message m = ds.sendMessage("guest-001", Map.of(
                "subject", "Photo attached", "body", "see image",
                "attachmentUrls", List.of("https://x/1.png", "https://x/2.png")));
        assertThat(m.getAttachmentUrls()).hasSize(2);
    }

    @Test
    void mark_thread_resolved_unknown_id_returns_null() {
        assertThat(ds.markThreadResolved("not-a-thread")).isNull();
    }

    // ── preferences update branches ──────────────────────────────────────────

    @Test
    void update_preferences_with_quiet_hours_persists_them() {
        NotificationPreferences p = ds.updatePreferences("guest-001", Map.of(
                "language", "fr",
                "timezone", "Europe/Paris",
                "channelSettings", List.of(Map.of(
                        "channel", "SMS", "enabled", true,
                        "quietHoursStart", "22:00",
                        "quietHoursEnd", "07:00",
                        "quietHoursTimezone", "Europe/Paris"))));
        assertThat(p.getLanguage()).isEqualTo("fr");
        assertThat(p.getChannelSettings()).hasSize(1);
        assertThat(p.getChannelSettings().get(0).quietHours()).isNotNull();
    }

    @Test
    void update_preferences_without_quiet_hours_keeps_quiet_null() {
        NotificationPreferences p = ds.updatePreferences("guest-001", Map.of(
                "channelSettings", List.of(Map.of("channel", "EMAIL", "enabled", true))));
        assertThat(p.getChannelSettings().get(0).quietHours()).isNull();
    }

    @Test
    void update_preferences_with_category_prefs_persists_them() {
        NotificationPreferences p = ds.updatePreferences("guest-001", Map.of(
                "categoryPreferences", List.of(Map.of(
                        "category", "MARKETING",
                        "enabledChannels", List.of("EMAIL")))));
        assertThat(p.getCategoryPreferences()).hasSize(1);
        assertThat(p.getCategoryPreferences().get(0).category()).isEqualTo("MARKETING");
    }

    @Test
    void update_preferences_with_no_lists_only_updates_scalars() {
        NotificationPreferences p = ds.updatePreferences("guest-001", Map.of(
                "language", "es", "timezone", "Europe/Madrid"));
        assertThat(p.getLanguage()).isEqualTo("es");
        assertThat(p.getTimezone()).isEqualTo("Europe/Madrid");
    }

    @Test
    void find_or_create_preferences_returns_existing_for_seeded_guest() {
        NotificationPreferences p1 = ds.findOrCreatePreferences("guest-001");
        NotificationPreferences p2 = ds.findOrCreatePreferences("guest-001");
        assertThat(p1).isSameAs(p2);
    }

    // ── devices ──────────────────────────────────────────────────────────────

    @Test
    void register_device_then_remove_deactivates_it() {
        PushDevice d = ds.registerDevice("guest-001", "ANDROID", "tok", "1.0", "Pixel");
        assertThat(ds.removeDevice(d.getId())).isTrue();
        assertThat(ds.findDevices("guest-001")).noneMatch(x -> x.getId().equals(d.getId()));
    }

    @Test
    void remove_unknown_device_returns_false() {
        assertThat(ds.removeDevice("not-a-device")).isFalse();
    }

    @Test
    void find_devices_for_unknown_guest_returns_empty() {
        assertThat(ds.findDevices("not-a-guest")).isEmpty();
    }

    @Test
    void find_devices_excludes_inactive_devices() {
        PushDevice d = ds.registerDevice("guest-007", "WEB", "tok-web", null, "Browser");
        ds.removeDevice(d.getId());
        assertThat(ds.findDevices("guest-007")).noneMatch(x -> x.getId().equals(d.getId()));
    }
}
