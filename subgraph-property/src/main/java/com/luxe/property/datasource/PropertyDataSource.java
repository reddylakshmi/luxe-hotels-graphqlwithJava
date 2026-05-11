package com.luxe.property.datasource;

import com.luxe.property.schema.types.Brand;
import com.luxe.property.schema.types.DestinationSuggestion;
import com.luxe.property.schema.types.Hotel;
import com.luxe.property.schema.types.HotelFacets;
import com.luxe.property.schema.types.Restaurant;
import com.luxe.property.schema.types.Review;
import com.luxe.property.schema.types.RoomType;
import com.luxe.property.schema.types.Spa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface PropertyDataSource {
    Optional<Hotel> getHotelById(String id);
    Optional<Hotel> getHotelBySlug(String slug);
    List<Hotel> searchHotels(Map<String, Object> filter, String sortBy);
    List<Hotel> getFeaturedHotels(String brandTier, String countryCode, int limit);

    /**
     * Batched id → Hotel lookup. The mock implementation falls
     * through to {@link #getHotelById(String)} per key — but the
     * federated {@code _entities} fetcher and any future DataLoader
     * needs *one* call per request, not N. Implementations backed
     * by a real DB / REST should fire a single SELECT IN (...) or
     * one mget-style batch round-trip.
     */
    default Map<String, Hotel> getHotelsByIds(Set<String> ids) {
        Map<String, Hotel> out = new HashMap<>();
        for (String id : ids) getHotelById(id).ifPresent(h -> out.put(id, h));
        return out;
    }
    /** Per-filter-option counts for the current search context. */
    HotelFacets computeFacets(Map<String, Object> filter);

    /**
     * Lightweight typeahead — returns up to {@code limit} suggestions matching
     * the partial query against hotel names, city names, and country names.
     * Empty / blank queries return an empty list.
     */
    List<DestinationSuggestion> destinationSuggestions(String query, int limit);

    Optional<Brand> getBrandById(String id);
    Optional<Brand> getBrandByCode(String code);
    List<Brand> getAllBrands(String tier);

    /** Batched brand-id lookup — see {@link #getHotelsByIds(Set)}. */
    default Map<String, Brand> getBrandsByIds(Set<String> ids) {
        Map<String, Brand> out = new HashMap<>();
        for (String id : ids) getBrandById(id).ifPresent(b -> out.put(id, b));
        return out;
    }

    Optional<RoomType> getRoomTypeById(String id);
    List<RoomType> getRoomTypesByHotelId(String hotelId);

    /** Batched per-hotel room-type lookup — see {@link #getHotelsByIds(Set)}. */
    default Map<String, List<RoomType>> getRoomTypesByHotelIds(Set<String> hotelIds) {
        Map<String, List<RoomType>> out = new HashMap<>();
        for (String hid : hotelIds) out.put(hid, getRoomTypesByHotelId(hid));
        return out;
    }

    Optional<Restaurant> getRestaurantById(String id);
    Optional<Spa> getSpaById(String id);

    List<Review> getReviewsByHotelId(String hotelId, String sortBy);
    Optional<Review> getReviewById(String id);
    Review submitReview(String hotelId, String guestName, double overallRating,
                        Map<String, Object> categoryRatings, String title, String body, String stayDate);
    Optional<Review> markReviewHelpful(String reviewId);
}
