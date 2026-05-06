package com.luxe.notifications.resolver;

import com.luxe.notifications.schema.types.GuestProfile;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthContextResolver;
import com.luxe.common.error.NotFoundError;
import com.luxe.common.error.ValidationError;
import com.luxe.common.error.FieldError;
import com.luxe.common.pagination.Connection;
import com.luxe.notifications.datasource.NotificationsDataSource;
import com.luxe.notifications.schema.types.*;
import com.netflix.graphql.dgs.*;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DgsComponent
public class NotificationsDataFetcher {

    private final NotificationsDataSource dataSource;
    private final AuthContextResolver authResolver;

    public NotificationsDataFetcher(NotificationsDataSource dataSource, AuthContextResolver authResolver) {
        this.dataSource = dataSource;
        this.authResolver = authResolver;
    }

    private AuthContext getAuth(DataFetchingEnvironment dfe) {
        return authResolver.resolve(dfe);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @DgsQuery
    public Object myNotifications(@InputArgument Integer first, @InputArgument String after,
                                    @InputArgument Map<String, Object> filter,
                                    DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        List<Notification> all = dataSource.findByGuestId(auth.guestId(), filter);
        Connection<Notification> conn = Connection.of(all, first != null ? first : 20, after);
        Map<String, Object> result = new HashMap<>();
        result.put("edges", conn.edges().stream()
                .map(e -> Map.of("node", e.node(), "cursor", e.cursor())).toList());
        result.put("pageInfo", pageInfo(conn));
        result.put("totalCount", conn.totalCount());
        result.put("unreadCount", dataSource.countUnread(auth.guestId()));
        return result;
    }

    @DgsQuery
    public Notification notification(@InputArgument String id) {
        return dataSource.findById(id).orElse(null);
    }

    @DgsQuery
    public int unreadCount(DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        return dataSource.countUnread(auth.guestId());
    }

    @DgsQuery
    public NotificationPreferences notificationPreferences(DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        return dataSource.findOrCreatePreferences(auth.guestId());
    }

    @DgsQuery
    public List<MessageThread> myMessageThreads(@InputArgument Integer first,
                                                  @InputArgument String status,
                                                  DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        List<MessageThread> all = dataSource.findThreads(auth.guestId(), status);
        return first != null ? all.stream().limit(first).toList() : all;
    }

    @DgsQuery
    public MessageThread messageThread(@InputArgument String id) {
        return dataSource.findThread(id).orElse(null);
    }

    @DgsQuery
    public List<PushDevice> myDevices(DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        return dataSource.findDevices(auth.guestId());
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @DgsMutation
    public PushDevice registerPushDevice(@InputArgument Map<String, Object> input,
                                           DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        return dataSource.registerDevice(auth.guestId(),
                (String) input.get("platform"),
                (String) input.get("pushToken"),
                (String) input.get("appVersion"),
                (String) input.get("deviceModel"));
    }

    @DgsMutation
    public boolean removePushDevice(@InputArgument String deviceId,
                                      DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        return dataSource.removeDevice(deviceId);
    }

    @DgsMutation
    public NotificationPreferences updateNotificationPreferences(@InputArgument Map<String, Object> input,
                                                                    DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        return dataSource.updatePreferences(auth.guestId(), input);
    }

    @DgsMutation
    public Notification markNotificationRead(@InputArgument String id,
                                               DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        return dataSource.markRead(id).orElse(null);
    }

    @DgsMutation
    public int markAllRead(DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        return dataSource.markAllRead(auth.guestId());
    }

    @DgsMutation
    public Object sendMessage(@InputArgument Map<String, Object> input,
                                DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        if (input.get("body") == null) {
            return new ValidationError("INVALID_INPUT", "Message body is required",
                    List.of(new FieldError("body", "Required")));
        }
        Message m = dataSource.sendMessage(auth.guestId(), input);
        if (m == null) return new NotFoundError("MessageThread",
                String.valueOf(input.get("threadId")));
        return m;
    }

    @DgsMutation
    public MessageThread markThreadResolved(@InputArgument String threadId,
                                              DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        return dataSource.markThreadResolved(threadId);
    }

    // ── MessageThread.messages with first arg ─────────────────────────────────

    @DgsData(parentType = "MessageThread", field = "messages")
    public List<Message> threadMessages(DataFetchingEnvironment dfe) {
        MessageThread t = dfe.getSource();
        Integer first = dfe.getArgument("first");
        return first == null ? t.getMessages()
                : t.getMessages().stream().limit(first).toList();
    }

    // ── GuestProfile.notifications federation ─────────────────────────────────

    @DgsData(parentType = "GuestProfile", field = "notifications")
    public List<Notification> guestNotifications(DataFetchingEnvironment dfe) {
        GuestProfile guest = dfe.getSource();
        Integer first = dfe.getArgument("first");
        List<Notification> all = dataSource.findByGuestId(guest.getId(), null);
        return first == null ? all : all.stream().limit(first).toList();
    }

    @DgsData(parentType = "GuestProfile", field = "unreadNotificationsCount")
    public int guestUnread(DataFetchingEnvironment dfe) {
        GuestProfile guest = dfe.getSource();
        return dataSource.countUnread(guest.getId());
    }

    @DgsEntityFetcher(name = "Notification")
    public Notification fetchNotification(Map<String, Object> values) {
        return dataSource.findById((String) values.get("id")).orElse(null);
    }

    @DgsEntityFetcher(name = "MessageThread")
    public MessageThread fetchThread(Map<String, Object> values) {
        return dataSource.findThread((String) values.get("id")).orElse(null);
    }

    private Map<String, Object> pageInfo(Connection<?> conn) {
        HashMap<String, Object> m = new HashMap<>();
        m.put("hasNextPage", conn.pageInfo().hasNextPage());
        m.put("hasPreviousPage", conn.pageInfo().hasPreviousPage());
        m.put("startCursor", conn.pageInfo().startCursor());
        m.put("endCursor", conn.pageInfo().endCursor());
        return m;
    }
    @DgsEntityFetcher(name = "GuestProfile")
    public GuestProfile fetchGuestProfileReference(Map<String, Object> values) {
        return new GuestProfile((String) values.get("id"));
    }

}
