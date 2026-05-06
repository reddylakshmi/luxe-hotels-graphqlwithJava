package com.luxe.experiences.schema.types;

import java.time.LocalDate;
import java.util.List;

public record ExperienceAvailability(
        String experienceId, LocalDate date, int partySize,
        List<TimeSlot> slots, boolean fullyBooked, LocalDate nextAvailableDate
) {}
