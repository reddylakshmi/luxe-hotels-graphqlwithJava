package com.luxe.property.schema.types;

public record HotelContact(
        String phone, String email, String website,
        String reservations, String whatsapp
) {}
