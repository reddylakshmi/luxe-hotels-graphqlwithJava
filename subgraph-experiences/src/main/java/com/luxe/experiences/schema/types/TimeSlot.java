package com.luxe.experiences.schema.types;

import com.luxe.common.scalar.Money;

public record TimeSlot(
        String slotToken, String startTime, String endTime,
        boolean available, int remainingCapacity, Money pricePerPerson, String guideName
) {}
