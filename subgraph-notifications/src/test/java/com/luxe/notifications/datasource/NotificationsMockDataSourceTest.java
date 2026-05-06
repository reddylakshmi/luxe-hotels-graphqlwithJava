package com.luxe.notifications.datasource;

import com.luxe.notifications.schema.types.MessageThread;
import com.luxe.notifications.schema.types.Notification;
import com.luxe.notifications.schema.types.PushDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationsMockDataSourceTest {

    private NotificationsMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new NotificationsMockDataSource();
    }

    @Test
    void find_by_guest_id_returns_only_that_guests_notifications() {
        List<Notification> mine = ds.findByGuestId("guest-001", null);
        assertThat(mine).isNotEmpty();
        assertThat(mine).allSatisfy(n -> assertThat(n.getGuestId()).isEqualTo("guest-001"));
    }

    @Test
    void find_by_guest_id_filters_by_unread_only() {
        List<Notification> unread = ds.findByGuestId("guest-001",
                Map.of("unreadOnly", true));
        assertThat(unread).allSatisfy(n -> assertThat(n.getStatus()).isNotEqualTo("READ"));
    }

    @Test
    void mark_read_updates_status() {
        Notification any = ds.findByGuestId("guest-001", null).get(0);
        ds.markRead(any.getId());
        assertThat(ds.findById(any.getId()).orElseThrow().getStatus()).isEqualTo("READ");
    }

    @Test
    void mark_all_read_returns_count_marked() {
        int marked = ds.markAllRead("guest-001");
        assertThat(marked).isGreaterThanOrEqualTo(0);
        assertThat(ds.countUnread("guest-001")).isZero();
    }

    @Test
    void register_device_creates_active_device() {
        PushDevice d = ds.registerDevice("guest-001", "IOS", "tok-1", "1.0", "iPhone");
        assertThat(d.isActive()).isTrue();
        assertThat(ds.findDevices("guest-001")).contains(d);
    }

    @Test
    void remove_device_deactivates() {
        PushDevice d = ds.registerDevice("guest-001", "IOS", "tok-1", "1.0", "iPhone");
        assertThat(ds.removeDevice(d.getId())).isTrue();
        assertThat(ds.findDevices("guest-001")).doesNotContain(d);
    }

    @Test
    void find_threads_filters_to_guest() {
        List<MessageThread> mine = ds.findThreads("guest-001", null);
        assertThat(mine).allSatisfy(t -> assertThat(t.getGuestId()).isEqualTo("guest-001"));
    }

    @Test
    void mark_thread_resolved_sets_status() {
        var thread = ds.findThreads("guest-001", null);
        if (!thread.isEmpty()) {
            String tid = thread.get(0).getId();
            ds.markThreadResolved(tid);
            assertThat(ds.findThread(tid).orElseThrow().getStatus()).isEqualTo("RESOLVED");
        }
    }

    @Test
    void find_or_create_preferences_returns_default_for_unknown_guest() {
        var prefs = ds.findOrCreatePreferences("brand-new-guest");
        assertThat(prefs).isNotNull();
        assertThat(prefs.getChannelSettings()).isNotEmpty();
    }
}
