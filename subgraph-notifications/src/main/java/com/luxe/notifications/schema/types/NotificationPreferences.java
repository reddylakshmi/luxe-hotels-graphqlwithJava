package com.luxe.notifications.schema.types;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationPreferences {
    private final String id, guestId;
    private List<ChannelSetting> channelSettings;
    private List<CategoryPreference> categoryPreferences;
    private String language, timezone;
    private OffsetDateTime updatedAt;

    public NotificationPreferences(String id, String guestId,
                                    List<ChannelSetting> channelSettings,
                                    List<CategoryPreference> categoryPreferences,
                                    String language, String timezone) {
        this.id = id; this.guestId = guestId;
        this.channelSettings = new ArrayList<>(channelSettings);
        this.categoryPreferences = new ArrayList<>(categoryPreferences);
        this.language = language; this.timezone = timezone;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getId() { return id; }
    public String getGuestId() { return guestId; }
    public List<ChannelSetting> getChannelSettings() { return channelSettings; }
    public List<CategoryPreference> getCategoryPreferences() { return categoryPreferences; }
    public String getLanguage() { return language; }
    public String getTimezone() { return timezone; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void apply(String language, String timezone,
                       List<ChannelSetting> channels, List<CategoryPreference> categories) {
        if (language != null) this.language = language;
        if (timezone != null) this.timezone = timezone;
        if (channels != null) this.channelSettings = channels;
        if (categories != null) this.categoryPreferences = categories;
        this.updatedAt = OffsetDateTime.now();
    }

    public record ChannelSetting(
            String channel, boolean enabled, QuietHours quietHours, String preferredAddress
    ) {}

    public record QuietHours(String start, String end, String timezone) {}

    public record CategoryPreference(String category, List<String> enabledChannels) {}
}
