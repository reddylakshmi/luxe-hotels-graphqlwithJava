package com.luxe.guest.schema.types;

public record GuestAddress(
        String id, String type, String line1, String line2,
        String city, String stateCode, String postalCode,
        String countryCode, boolean isPrimary
) {}
