package com.luxe.property.schema.types;

public record Address(
        String line1, String line2,
        String city, String state, String postalCode,
        String countryCode, String countryName
) {}
