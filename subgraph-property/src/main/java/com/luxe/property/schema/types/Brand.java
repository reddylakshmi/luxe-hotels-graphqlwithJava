package com.luxe.property.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.OffsetDateTime;
import java.util.List;

public class Brand implements HasId {
    private final String id, code, name, slug, tier, tagline, description;
    private final String logoUrl, heroImageUrl, accentColor;
    private final double loyaltyPointsMultiplier;
    private final int numberOfProperties;
    private final String sustainabilityCommitment;
    private final OffsetDateTime createdAt, updatedAt;

    public Brand(String id, String code, String name, String slug, String tier,
                 String tagline, String description, String logoUrl, String heroImageUrl,
                 String accentColor, double loyaltyPointsMultiplier, int numberOfProperties,
                 String sustainabilityCommitment, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id; this.code = code; this.name = name; this.slug = slug; this.tier = tier;
        this.tagline = tagline; this.description = description; this.logoUrl = logoUrl;
        this.heroImageUrl = heroImageUrl; this.accentColor = accentColor;
        this.loyaltyPointsMultiplier = loyaltyPointsMultiplier;
        this.numberOfProperties = numberOfProperties;
        this.sustainabilityCommitment = sustainabilityCommitment;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    @Override public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getTier() { return tier; }
    public String getTagline() { return tagline; }
    public String getDescription() { return description; }
    public String getLogoUrl() { return logoUrl; }
    public String getHeroImageUrl() { return heroImageUrl; }
    public String getAccentColor() { return accentColor; }
    public double getLoyaltyPointsMultiplier() { return loyaltyPointsMultiplier; }
    public int getNumberOfProperties() { return numberOfProperties; }
    public String getSustainabilityCommitment() { return sustainabilityCommitment; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
