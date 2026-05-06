package com.luxe.property.schema.types;

import java.time.OffsetDateTime;

public class Review {
    private final String id, hotelId, guestName, stayDate, title, body, language;
    private final double overallRating;
    private final CategoryRatings categoryRatings;
    private final boolean verified;
    private int helpfulCount;
    private final ReviewResponse response;

    public Review(String id, String hotelId, String guestName, String stayDate,
                  double overallRating, CategoryRatings categoryRatings, String title,
                  String body, String language, boolean verified, int helpfulCount,
                  ReviewResponse response) {
        this.id = id; this.hotelId = hotelId; this.guestName = guestName;
        this.stayDate = stayDate; this.overallRating = overallRating;
        this.categoryRatings = categoryRatings; this.title = title; this.body = body;
        this.language = language; this.verified = verified;
        this.helpfulCount = helpfulCount; this.response = response;
    }

    public String getId() { return id; }
    public String getHotelId() { return hotelId; }
    public String getGuestName() { return guestName; }
    public String getStayDate() { return stayDate; }
    public double getOverallRating() { return overallRating; }
    public CategoryRatings getCategoryRatings() { return categoryRatings; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getLanguage() { return language; }
    public boolean isVerified() { return verified; }
    public int getHelpfulCount() { return helpfulCount; }
    public ReviewResponse getResponse() { return response; }
    public void incrementHelpful() { this.helpfulCount++; }
}
