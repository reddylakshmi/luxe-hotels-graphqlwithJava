package com.luxe.content.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class Article implements HasId {
    private final String id, slug, category, status;
    private final List<LocalizedContent.LocaleText> titleTranslations;
    private final List<LocalizedContent.LocaleText> subtitleTranslations;
    private final List<LocalizedContent.LocaleText> bodyTranslations;
    private final List<LocalizedContent.LocaleText> excerptTranslations;
    private final MediaAsset heroImage;
    private final List<MediaAsset> gallery;
    private final ContentAuthor author;
    private final List<String> tags;
    private final List<String> relatedHotelIds;
    private final int readTimeMinutes;
    private final OffsetDateTime publishedAt, updatedAt;

    private String requestedLocale = "en";

    public Article(String id, String slug, String category, String status,
                   List<LocalizedContent.LocaleText> title,
                   List<LocalizedContent.LocaleText> subtitle,
                   List<LocalizedContent.LocaleText> body,
                   List<LocalizedContent.LocaleText> excerpt,
                   MediaAsset heroImage, List<MediaAsset> gallery,
                   ContentAuthor author, List<String> tags,
                   List<String> relatedHotelIds, int readTimeMinutes,
                   OffsetDateTime publishedAt, OffsetDateTime updatedAt) {
        this.id = id; this.slug = slug; this.category = category; this.status = status;
        this.titleTranslations = title; this.subtitleTranslations = subtitle;
        this.bodyTranslations = body; this.excerptTranslations = excerpt;
        this.heroImage = heroImage; this.gallery = gallery;
        this.author = author; this.tags = tags;
        this.relatedHotelIds = relatedHotelIds;
        this.readTimeMinutes = readTimeMinutes;
        this.publishedAt = publishedAt; this.updatedAt = updatedAt;
    }

    @Override public String getId() { return id; }
    public String getSlug() { return slug; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public MediaAsset getHeroImage() { return heroImage; }
    public List<MediaAsset> getGallery() { return gallery; }
    public ContentAuthor getAuthor() { return author; }
    public List<String> getTags() { return tags; }
    public int getReadTimeMinutes() { return readTimeMinutes; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public List<Map<String, Object>> getRelatedHotels() {
        return relatedHotelIds.stream().<Map<String, Object>>map(id -> Map.of("id", id)).toList();
    }

    public LocalizedContent getTitle() { return LocalizedContent.of(requestedLocale, "", titleTranslations); }
    public LocalizedContent getSubtitle() {
        return subtitleTranslations == null || subtitleTranslations.isEmpty() ? null
                : LocalizedContent.of(requestedLocale, "", subtitleTranslations);
    }
    public LocalizedContent getBody() { return LocalizedContent.of(requestedLocale, "", bodyTranslations); }
    public LocalizedContent getExcerpt() { return LocalizedContent.of(requestedLocale, "", excerptTranslations); }

    public Article forLocale(String locale) {
        if (locale != null) this.requestedLocale = locale;
        return this;
    }
}
