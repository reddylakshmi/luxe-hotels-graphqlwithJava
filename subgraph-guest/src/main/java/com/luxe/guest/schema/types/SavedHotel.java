package com.luxe.guest.schema.types;

import java.time.OffsetDateTime;

public record SavedHotel(String id, String hotelId, OffsetDateTime savedAt) {}
