package com.luxe.reservations.schema.types;

import java.time.LocalDate;
import java.util.List;

public record CheckInEligibility(
        boolean eligible, String reservationId, LocalDate checkInDate,
        List<String> reasons, boolean onlineCheckInAvailable, boolean mobileKeyAvailable
) {}
