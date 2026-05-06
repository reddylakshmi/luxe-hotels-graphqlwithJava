package com.luxe.property.schema.types;

public record HotelPolicies(
        CheckInPolicy checkIn, CheckOutPolicy checkOut,
        CancellationPolicy cancellation, PetPolicy petPolicy,
        String smokingPolicy, String childPolicy
) {
    public record CheckInPolicy(String standardTime, EarlyLatePolicy earlyCheckIn) {}
    public record CheckOutPolicy(String standardTime, EarlyLatePolicy lateCheckOut) {}
    public record EarlyLatePolicy(boolean available, String fee, boolean subjectToAvailability) {}
    public record CancellationPolicy(Integer freeCancellationHours, String defaultPolicy) {}
    public record PetPolicy(boolean allowed, String fee, String restrictions) {}
}
