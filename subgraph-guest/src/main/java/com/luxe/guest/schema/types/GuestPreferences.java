package com.luxe.guest.schema.types;

import java.util.List;

public record GuestPreferences(
        RoomPreferences room,
        BedPreferences bed,
        PillowPreferences pillow,
        DietaryPreferences dietary,
        AccessibilityNeeds accessibility,
        TransportPreferences transport,
        CommunicationPreferences communication
) {
    public static GuestPreferences defaults() {
        return new GuestPreferences(
                new RoomPreferences(List.of(), null, null, false, false, false, false),
                new BedPreferences(null, null),
                new PillowPreferences(null, null),
                new DietaryPreferences(List.of(), List.of(), false, false, false),
                new AccessibilityNeeds(false, false, false),
                new TransportPreferences(null, List.of()),
                new CommunicationPreferences("en", "EMAIL", true, false)
        );
    }

    public record RoomPreferences(
            List<String> preferredTypes, String viewPreference, String floorPreference,
            Boolean quietRoom, Boolean highFloor, Boolean awayFromElevator, Boolean adjacentRooms
    ) {}

    public record BedPreferences(String type, String preferredConfiguration) {}

    public record PillowPreferences(String firmness, String filling) {}

    public record DietaryPreferences(
            List<String> restrictions, List<String> allergies,
            Boolean kosher, Boolean halal, Boolean vegan
    ) {}

    public record AccessibilityNeeds(
            Boolean wheelchairAccessible, Boolean rollInShower, Boolean hearingAccessible
    ) {}

    public record TransportPreferences(
            String preferredMode, List<FrequentFlyerNumber> frequentFlyerNumbers
    ) {}

    public record CommunicationPreferences(
            String preferredLanguage, String preferredChannel,
            boolean marketingOptIn, boolean doNotDisturb
    ) {}
}
