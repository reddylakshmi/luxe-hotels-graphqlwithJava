package com.luxe.property.datasource;

import com.luxe.property.schema.types.Brand;
import com.luxe.property.schema.types.Hotel;
import com.luxe.property.schema.types.Restaurant;
import com.luxe.property.schema.types.Review;
import com.luxe.property.schema.types.RoomType;
import com.luxe.property.schema.types.Spa;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PropertyDataSource {
    Optional<Hotel> getHotelById(String id);
    Optional<Hotel> getHotelBySlug(String slug);
    List<Hotel> searchHotels(Map<String, Object> filter, String sortBy);
    List<Hotel> getFeaturedHotels(String brandTier, String countryCode, int limit);

    Optional<Brand> getBrandById(String id);
    Optional<Brand> getBrandByCode(String code);
    List<Brand> getAllBrands(String tier);

    Optional<RoomType> getRoomTypeById(String id);
    List<RoomType> getRoomTypesByHotelId(String hotelId);

    Optional<Restaurant> getRestaurantById(String id);
    Optional<Spa> getSpaById(String id);

    List<Review> getReviewsByHotelId(String hotelId, String sortBy);
    Optional<Review> getReviewById(String id);
    Review submitReview(String hotelId, String guestName, double overallRating,
                        Map<String, Object> categoryRatings, String title, String body, String stayDate);
    Optional<Review> markReviewHelpful(String reviewId);
}
