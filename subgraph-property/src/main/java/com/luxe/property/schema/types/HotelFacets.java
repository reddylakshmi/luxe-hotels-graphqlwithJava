package com.luxe.property.schema.types;

import java.util.List;

/**
 * Per-filter-option counts for the search results page. Each list contains
 * one entry per filter value with the number of hotels in the current search
 * context that would still match if the user picked it.
 */
public record HotelFacets(
        int totalCount,
        List<BrandFacet> byBrand,
        List<TierFacet> byBrandTier,
        List<CityFacet> byCity,
        AmenityFacets amenities,
        List<GuestRatingBucket> guestRating
) {

    public record BrandFacet(String brandId, Brand brand, int count) {}

    public record TierFacet(String tier, int count) {}

    public record CityFacet(String city, String countryCode, int count) {}

    public record AmenityFacets(
            int hasFreeBreakfast,
            int hasPool,
            int hasSpa,
            int hasGolf,
            int petsAllowed
    ) {}

    public record GuestRatingBucket(double minRating, int count) {}
}
