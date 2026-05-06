package com.luxe.experiences.schema.types;

import java.time.LocalDate;
import java.util.List;

public record GolfTeeTimeAvailability(
        String hotelId, String courseId, String courseName,
        LocalDate date, int players, List<TimeSlot> slots
) {}
