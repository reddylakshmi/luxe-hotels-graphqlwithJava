package com.luxe.property.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.OffsetDateTime;
import java.util.List;

public class Hotel implements HasId {
    private final String id, brandId, chainCode, propertyCode, name, slug, status;
    private final Integer openedYear, renovatedYear, numberOfFloors;
    private final int totalRooms, starRating;
    private final GuestRating guestRating;
    private final HotelLocation location;
    private final HotelContact contact;
    private final LocalizedContent description;
    private final HotelPolicies policies;
    private final List<Amenity> amenities;
    private final List<RoomType> roomTypes;
    private final List<Restaurant> restaurants;
    private final Spa spa;
    private final GolfCourse golfCourse;
    private final List<MediaAsset> media;
    private final List<Attraction> nearbyAttractions;
    private final ParkingInfo parkingInfo;
    private final SustainabilityInfo sustainabilityInfo;
    private final List<Award> awards;
    private final boolean isFeatured;
    private final OffsetDateTime updatedAt;

    public Hotel(String id, String brandId, String chainCode, String propertyCode, String name,
                 String slug, String status, Integer openedYear, Integer renovatedYear,
                 Integer numberOfFloors, int totalRooms, int starRating, GuestRating guestRating,
                 HotelLocation location, HotelContact contact, LocalizedContent description,
                 HotelPolicies policies, List<Amenity> amenities, List<RoomType> roomTypes,
                 List<Restaurant> restaurants, Spa spa, GolfCourse golfCourse,
                 List<MediaAsset> media, List<Attraction> nearbyAttractions,
                 ParkingInfo parkingInfo, SustainabilityInfo sustainabilityInfo,
                 List<Award> awards, boolean isFeatured, OffsetDateTime updatedAt) {
        this.id = id; this.brandId = brandId; this.chainCode = chainCode;
        this.propertyCode = propertyCode; this.name = name; this.slug = slug;
        this.status = status; this.openedYear = openedYear; this.renovatedYear = renovatedYear;
        this.numberOfFloors = numberOfFloors; this.totalRooms = totalRooms;
        this.starRating = starRating; this.guestRating = guestRating;
        this.location = location; this.contact = contact; this.description = description;
        this.policies = policies; this.amenities = amenities; this.roomTypes = roomTypes;
        this.restaurants = restaurants; this.spa = spa; this.golfCourse = golfCourse;
        this.media = media; this.nearbyAttractions = nearbyAttractions;
        this.parkingInfo = parkingInfo; this.sustainabilityInfo = sustainabilityInfo;
        this.awards = awards; this.isFeatured = isFeatured; this.updatedAt = updatedAt;
    }

    @Override public String getId() { return id; }
    public String getBrandId() { return brandId; }
    public String getChainCode() { return chainCode; }
    public String getPropertyCode() { return propertyCode; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getStatus() { return status; }
    public Integer getOpenedYear() { return openedYear; }
    public Integer getRenovatedYear() { return renovatedYear; }
    public Integer getNumberOfFloors() { return numberOfFloors; }
    public int getTotalRooms() { return totalRooms; }
    public int getStarRating() { return starRating; }
    public GuestRating getGuestRating() { return guestRating; }
    public HotelLocation getLocation() { return location; }
    public HotelContact getContact() { return contact; }
    public LocalizedContent getDescription() { return description; }
    public HotelPolicies getPolicies() { return policies; }
    public List<Amenity> getAmenities() { return amenities; }
    public List<RoomType> getRoomTypes() { return roomTypes; }
    public List<Restaurant> getRestaurants() { return restaurants; }
    public Spa getSpa() { return spa; }
    public GolfCourse getGolfCourse() { return golfCourse; }
    public List<MediaAsset> getMedia() { return media; }
    public List<Attraction> getNearbyAttractions() { return nearbyAttractions; }
    public ParkingInfo getParkingInfo() { return parkingInfo; }
    public SustainabilityInfo getSustainabilityInfo() { return sustainabilityInfo; }
    public List<Award> getAwards() { return awards; }
    public boolean isIsFeatured() { return isFeatured; }
    public boolean getIsFeatured() { return isFeatured; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public boolean isHasRestaurants() { return restaurants != null && !restaurants.isEmpty(); }
    public boolean isHasSpa() { return spa != null; }
    public boolean isHasGolf() { return golfCourse != null; }
    public boolean isHasPool() {
        return amenities != null && amenities.stream()
                .anyMatch(a -> "POOL".equals(a.getCategory()));
    }
    public boolean isHasFreeBreakfast() {
        return amenities != null && amenities.stream()
                .anyMatch(a -> "BREAKFAST".equals(a.getCode()));
    }
    public boolean isPetsAllowed() {
        return policies != null && policies.petPolicy() != null && policies.petPolicy().allowed();
    }
}
