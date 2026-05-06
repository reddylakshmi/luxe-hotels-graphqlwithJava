package com.luxe.property.schema.types;

import com.luxe.common.pagination.HasId;
import java.util.List;

public class RoomType implements HasId {
    private final String id, hotelId, code, name, category, view, floor;
    private final LocalizedContent description;
    private final Double sizeSqm;
    private final OccupancyLimit maxOccupancy;
    private final List<BedConfiguration> bedConfiguration;
    private final List<Amenity> amenities;
    private final List<MediaAsset> media;
    private final List<String> accessibilityFeatures, highlights;
    private final boolean isSmokingAllowed, connectingRoomsAvailable;

    public RoomType(String id, String hotelId, String code, String name, String category,
                    LocalizedContent description, Double sizeSqm, OccupancyLimit maxOccupancy,
                    List<BedConfiguration> bedConfiguration, String view, String floor,
                    List<Amenity> amenities, List<MediaAsset> media,
                    List<String> accessibilityFeatures, List<String> highlights,
                    boolean isSmokingAllowed, boolean connectingRoomsAvailable) {
        this.id = id; this.hotelId = hotelId; this.code = code; this.name = name;
        this.category = category; this.description = description; this.sizeSqm = sizeSqm;
        this.maxOccupancy = maxOccupancy; this.bedConfiguration = bedConfiguration;
        this.view = view; this.floor = floor; this.amenities = amenities; this.media = media;
        this.accessibilityFeatures = accessibilityFeatures; this.highlights = highlights;
        this.isSmokingAllowed = isSmokingAllowed;
        this.connectingRoomsAvailable = connectingRoomsAvailable;
    }

    @Override public String getId() { return id; }
    public String getHotelId() { return hotelId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public LocalizedContent getDescription() { return description; }
    public Double getSizeSqm() { return sizeSqm; }
    public OccupancyLimit getMaxOccupancy() { return maxOccupancy; }
    public List<BedConfiguration> getBedConfiguration() { return bedConfiguration; }
    public String getView() { return view; }
    public String getFloor() { return floor; }
    public List<Amenity> getAmenities() { return amenities; }
    public List<MediaAsset> getMedia() { return media; }
    public List<String> getAccessibilityFeatures() { return accessibilityFeatures; }
    public List<String> getHighlights() { return highlights; }
    public boolean isIsSmokingAllowed() { return isSmokingAllowed; }
    public boolean isConnectingRoomsAvailable() { return connectingRoomsAvailable; }
}
