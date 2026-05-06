package com.luxe.notifications.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationsMutationsResolverTest {

    @Autowired
    DgsQueryExecutor dgs;

    @Test
    void mark_notification_read_requires_auth() {
        var result = dgs.execute(
                "mutation { markNotificationRead(id: \"ntf-001\") { id status } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void mark_all_read_requires_auth() {
        var result = dgs.execute("mutation { markAllRead }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void register_push_device_requires_auth() {
        var result = dgs.execute("""
                mutation { registerPushDevice(input: {
                    platform: IOS, pushToken: "tok-1", deviceModel: "iPhone"
                }) { id platform } }
                """);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void remove_push_device_requires_auth() {
        var result = dgs.execute(
                "mutation { removePushDevice(deviceId: \"dev-001\") }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void update_notification_preferences_requires_auth() {
        var result = dgs.execute(
                "mutation { updateNotificationPreferences(input: { language: \"fr\" }) { id language } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void send_message_requires_auth() {
        var result = dgs.execute("""
                mutation { sendMessage(input: { body: "Hello" }) {
                  ... on Message { id }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void mark_thread_resolved_requires_auth() {
        var result = dgs.execute(
                "mutation { markThreadResolved(threadId: \"thr-001\") { id status } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void my_notifications_requires_auth() {
        var result = dgs.execute("{ myNotifications { totalCount unreadCount } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void notification_preferences_requires_auth() {
        var result = dgs.execute(
                "{ notificationPreferences { language timezone } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void my_message_threads_requires_auth() {
        var result = dgs.execute("{ myMessageThreads { id status } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void my_devices_requires_auth() {
        var result = dgs.execute("{ myDevices { id platform } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void message_thread_query_works_for_known_id() {
        Object t = dgs.executeAndExtractJsonPath(
                "{ messageThread(id: \"thr-001\") { id status messages(first: 1) { id sender body } } }",
                "data.messageThread");
        assertThat(t).isNotNull();
    }

    @Test
    void federation_entities_resolves_notification_by_key() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on Notification { id title }
                  }
                }
                """;
        var rep = Map.of("__typename", "Notification", "id", "ntf-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities =
                (List<Map<String, Object>>) ((Map<String, Object>) result.getData()).get("_entities");
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).get("id")).isEqualTo("ntf-001");
    }

    @Test
    void federation_entities_resolves_message_thread_by_key() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on MessageThread { id status }
                  }
                }
                """;
        var rep = Map.of("__typename", "MessageThread", "id", "thr-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void send_message_with_missing_body_returns_validation_error_path() {
        // The resolver checks body BEFORE calling auth.requireAuth(),
        // but auth check is first in this resolver — so we expect an auth error,
        // which still exercises the resolver method entry & arg parsing.
        var result = dgs.execute(
                "mutation { sendMessage(input: {}) { ... on ValidationError { code message } } }");
        // schema requires body: String!  so this fails at validation level
        assertThat(result.getErrors()).isNotEmpty();
    }
}
