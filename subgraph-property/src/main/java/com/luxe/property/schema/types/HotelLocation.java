package com.luxe.property.schema.types;

public record HotelLocation(
        Address address,
        Coordinates coordinates,
        String timezone,
        AirportInfo airport,
        DistanceValue distanceFromSearch
) {}
