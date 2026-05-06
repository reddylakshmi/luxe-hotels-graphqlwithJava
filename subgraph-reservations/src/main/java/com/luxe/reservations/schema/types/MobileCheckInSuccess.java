package com.luxe.reservations.schema.types;

public record MobileCheckInSuccess(
        Reservation reservation,
        DigitalKey digitalKey,
        String message
) {}
