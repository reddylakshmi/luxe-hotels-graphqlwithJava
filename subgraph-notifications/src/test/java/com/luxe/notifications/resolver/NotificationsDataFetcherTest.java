package com.luxe.notifications.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationsDataFetcherTest {

    @Autowired
    DgsQueryExecutor dgs;

    @Test
    void my_notifications_requires_authentication() {
        var result = dgs.execute("{ myNotifications { totalCount } }");
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getMessage())
                .containsIgnoringCase("Authentication");
    }

    @Test
    void unread_count_requires_authentication() {
        var result = dgs.execute("{ unreadCount }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void notification_by_id_does_not_require_auth() {
        // ntf-001 is seeded for guest-001; resolver isn't auth-gated for direct lookup
        Object n = dgs.executeAndExtractJsonPath(
                "{ notification(id: \"ntf-001\") { id title } }",
                "data.notification");
        assertThat(n).isNotNull();
    }
}
