package com.luxe.property.schema.types;

import java.util.List;

public record GuestRating(
        double overall, int count,
        RatingBreakdown breakdown,
        List<TravelerTypeRating> travelerTypes
) {}
