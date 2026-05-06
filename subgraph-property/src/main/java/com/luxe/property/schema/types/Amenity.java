package com.luxe.property.schema.types;

public class Amenity {
    private final String id, code, name, category, description, iconUrl, availableHours, fee;
    private final boolean isPremium;

    public Amenity(String id, String code, String name, String category, String description,
                   String iconUrl, boolean isPremium, String fee, String availableHours) {
        this.id = id; this.code = code; this.name = name; this.category = category;
        this.description = description; this.iconUrl = iconUrl;
        this.isPremium = isPremium; this.fee = fee; this.availableHours = availableHours;
    }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getIconUrl() { return iconUrl; }
    public boolean isIsPremium() { return isPremium; }
    public String getFee() { return fee; }
    public String getAvailableHours() { return availableHours; }
}
