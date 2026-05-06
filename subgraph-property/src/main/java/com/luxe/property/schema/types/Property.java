package com.luxe.property.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.OffsetDateTime;
import java.util.List;

public class Property implements HasId {
    private String id;
    private String code;
    private String name;
    private String brand;
    private Location location;
    private int starRating;
    private List<Amenity> amenities;
    private List<RoomType> roomTypes;
    private String checkInTime;
    private String checkOutTime;
    private List<PropertyImage> images;
    private String status;
    private int totalRooms;
    private String description;
    private String phone;
    private String email;
    private String website;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Property() {}

    public Property(String id, String code, String name, String brand, Location location,
                    int starRating, List<Amenity> amenities, List<RoomType> roomTypes,
                    String checkInTime, String checkOutTime, List<PropertyImage> images,
                    String status, int totalRooms, String description,
                    String phone, String email, String website,
                    OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id; this.code = code; this.name = name; this.brand = brand;
        this.location = location; this.starRating = starRating; this.amenities = amenities;
        this.roomTypes = roomTypes; this.checkInTime = checkInTime; this.checkOutTime = checkOutTime;
        this.images = images; this.status = status; this.totalRooms = totalRooms;
        this.description = description; this.phone = phone; this.email = email;
        this.website = website; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    @Override public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public Location getLocation() { return location; }
    public int getStarRating() { return starRating; }
    public List<Amenity> getAmenities() { return amenities; }
    public List<RoomType> getRoomTypes() { return roomTypes; }
    public String getCheckInTime() { return checkInTime; }
    public String getCheckOutTime() { return checkOutTime; }
    public List<PropertyImage> getImages() { return images; }
    public String getStatus() { return status; }
    public int getTotalRooms() { return totalRooms; }
    public String getDescription() { return description; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getWebsite() { return website; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
