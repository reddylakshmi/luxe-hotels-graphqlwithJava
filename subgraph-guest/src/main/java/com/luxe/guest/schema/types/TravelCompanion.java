package com.luxe.guest.schema.types;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TravelCompanion(
        String id, GuestName name, String relationship,
        String email, String phone, String loyaltyNumber,
        LocalDate dateOfBirth, OffsetDateTime addedAt
) {}
