package com.luxe.content.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class TravelInspiration implements HasId {
    private final String id, slug, destination, region, bestSeason;
    private final List<LocalizedContent.LocaleText> titleTranslations;
    private final List<LocalizedContent.LocaleText> descriptionTranslations;
    private final MediaAsset heroImage;
    private final List<MediaAsset> gallery;
    private final List<List<LocalizedContent.LocaleText>> highlightTranslations;
    private final List<String> featuredHotelIds;
    private final Money approxBudget;
    private final int recommendedDays;
    private final OffsetDateTime publishedAt;
    private String requestedLocale = "en";

    public TravelInspiration(String id, String slug, String destination, String region,
                              String bestSeason,
                              List<LocalizedContent.LocaleText> title,
                              List<LocalizedContent.LocaleText> description,
                              MediaAsset heroImage, List<MediaAsset> gallery,
                              List<List<LocalizedContent.LocaleText>> highlights,
                              List<String> featuredHotelIds,
                              Money approxBudget, int recommendedDays,
                              OffsetDateTime publishedAt) {
        this.id = id; this.slug = slug; this.destination = destination; this.region = region;
        this.bestSeason = bestSeason;
        this.titleTranslations = title; this.descriptionTranslations = description;
        this.heroImage = heroImage; this.gallery = gallery;
        this.highlightTranslations = highlights;
        this.featuredHotelIds = featuredHotelIds;
        this.approxBudget = approxBudget; this.recommendedDays = recommendedDays;
        this.publishedAt = publishedAt;
    }

    @Override public String getId() { return id; }
    public String getSlug() { return slug; }
    public String getDestination() { return destination; }
    public String getRegion() { return region; }
    public String getBestSeason() { return bestSeason; }
    public MediaAsset getHeroImage() { return heroImage; }
    public List<MediaAsset> getGallery() { return gallery; }
    public Money getApproxBudget() { return approxBudget; }
    public int getRecommendedDays() { return recommendedDays; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }

    public LocalizedContent getTitle() { return LocalizedContent.of(requestedLocale, "", titleTranslations); }
    public LocalizedContent getDescription() { return LocalizedContent.of(requestedLocale, "", descriptionTranslations); }
    public List<LocalizedContent> getHighlights() {
        return highlightTranslations.stream()
                .map(t -> LocalizedContent.of(requestedLocale, "", t))
                .toList();
    }
    public List<Map<String, Object>> getFeaturedHotels() {
        return featuredHotelIds.stream().<Map<String, Object>>map(id -> Map.of("id", id)).toList();
    }

    public TravelInspiration forLocale(String locale) {
        if (locale != null) this.requestedLocale = locale;
        return this;
    }
}
