package com.luxe.notifications.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.OffsetDateTime;

public class PushDevice implements HasId {
    private final String id, guestId, platform, appVersion, deviceModel, pushToken;
    private final OffsetDateTime registeredAt;
    private OffsetDateTime lastSeenAt;
    private boolean active;

    public PushDevice(String id, String guestId, String platform, String appVersion,
                       String deviceModel, String pushToken,
                       OffsetDateTime registeredAt, OffsetDateTime lastSeenAt, boolean active) {
        this.id = id; this.guestId = guestId; this.platform = platform;
        this.appVersion = appVersion; this.deviceModel = deviceModel;
        this.pushToken = pushToken;
        this.registeredAt = registeredAt; this.lastSeenAt = lastSeenAt;
        this.active = active;
    }

    @Override public String getId() { return id; }
    public String getGuestId() { return guestId; }
    public String getPlatform() { return platform; }
    public String getAppVersion() { return appVersion; }
    public String getDeviceModel() { return deviceModel; }
    public String getPushToken() { return pushToken; }
    public OffsetDateTime getRegisteredAt() { return registeredAt; }
    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public boolean isActive() { return active; }
    public void deactivate() { this.active = false; }
    public void touch() { this.lastSeenAt = OffsetDateTime.now(); }
}
