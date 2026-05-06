package com.luxe.property.schema.types;

public record Location(
        String address1,
        String address2,
        String city,
        String state,
        String postalCode,
        String country,
        String countryCode,
        double latitude,
        double longitude,
        String timezone
) {}
