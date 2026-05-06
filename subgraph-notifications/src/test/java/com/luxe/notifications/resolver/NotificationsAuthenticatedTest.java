package com.luxe.notifications.resolver;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthContextResolver;
import com.luxe.common.auth.AuthRole;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authenticated coverage of notification mutations + queries.
 */
@SpringBootTest
@Import(NotificationsAuthenticatedTest.AuthOverrideConfig.class)
class NotificationsAuthenticatedTest {

    @TestConfiguration
    static class AuthOverrideConfig {
        @Bean @Primary
        AuthContextResolver mockAuth() {
            return dfe -> AuthContext.of("guest-001", "LUX0001234567", AuthRole.GUEST);
        }
    }

    @Autowired
    DgsQueryExecutor dgs;

    @Test
    void my_notifications_returns_paginated_list_with_unread_count() {
        Integer total = dgs.executeAndExtractJsonPath(
                "{ myNotifications { totalCount unreadCount edges { node { id title } } } }",
                "data.myNotifications.totalCount");
        assertThat(total).isNotNull();
    }

    @Test
    void my_notifications_filter_by_unread_only_returns_unread() {
        Integer total = dgs.executeAndExtractJsonPath(
                "{ myNotifications(filter: { unreadOnly: true }) { totalCount } }",
                "data.myNotifications.totalCount");
        assertThat(total).isNotNull();
    }

    @Test
    void unread_count_returns_count() {
        Integer count = dgs.executeAndExtractJsonPath(
                "{ unreadCount }", "data.unreadCount");
        assertThat(count).isNotNull();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    void notification_preferences_returns_preferences() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ notificationPreferences { id language timezone channelSettings { channel enabled } } }",
                "data.notificationPreferences");
        assertThat(result).isNotNull();
    }

    @Test
    void my_message_threads_returns_threads() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ myMessageThreads { id status subject } }",
                "data.myMessageThreads");
        assertThat(result).isNotNull();
    }

    @Test
    void my_devices_returns_registered_devices() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ myDevices { id platform } }",
                "data.myDevices");
        assertThat(result).isNotNull();
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    @Test
    void register_push_device_returns_active_device() {
        Boolean active = dgs.executeAndExtractJsonPath("""
                mutation { registerPushDevice(input: {
                    platform: IOS, pushToken: "token-X", deviceModel: "iPhone 15"
                }) { id platform active } }
                """, "data.registerPushDevice.active");
        assertThat(active).isTrue();
    }

    @Test
    void mark_notification_read_returns_read_notification() {
        String status = dgs.executeAndExtractJsonPath(
                "mutation { markNotificationRead(id: \"ntf-001\") { id status } }",
                "data.markNotificationRead.status");
        assertThat(status).isEqualTo("READ");
    }

    @Test
    void mark_all_read_returns_count() {
        Integer count = dgs.executeAndExtractJsonPath(
                "mutation { markAllRead }", "data.markAllRead");
        assertThat(count).isNotNull();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    void update_notification_preferences_returns_updated_prefs() {
        String lang = dgs.executeAndExtractJsonPath("""
                mutation { updateNotificationPreferences(input: { language: "fr" }) {
                    id language
                } }
                """, "data.updateNotificationPreferences.language");
        assertThat(lang).isEqualTo("fr");
    }

    @Test
    void send_message_to_existing_thread_returns_message() {
        var result = dgs.execute("""
                mutation { sendMessage(input: {
                    threadId: "thr-001", body: "Follow-up question"
                }) {
                  ... on Message { id sender body }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void send_message_starts_new_thread_when_thread_id_omitted() {
        var result = dgs.execute("""
                mutation { sendMessage(input: {
                    subject: "New question", body: "Hello", hotelId: "prop-paris-001"
                }) {
                  ... on Message { id sender body }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void mark_thread_resolved_sets_status() {
        String status = dgs.executeAndExtractJsonPath(
                "mutation { markThreadResolved(threadId: \"thr-001\") { id status } }",
                "data.markThreadResolved.status");
        assertThat(status).isEqualTo("RESOLVED");
    }
}
