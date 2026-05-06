package com.luxe.content.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.OffsetDateTime;
import java.util.List;

public class ContentCollection implements HasId {
    private final String id, slug;
    private final List<LocalizedContent.LocaleText> titleTranslations;
    private final List<LocalizedContent.LocaleText> descriptionTranslations;
    private final MediaAsset heroImage;
    private final List<String> articleIds;
    private final List<String> inspirationIds;
    private final List<String> spotlightIds;
    private final OffsetDateTime updatedAt;
    private String requestedLocale = "en";

    public ContentCollection(String id, String slug,
                              List<LocalizedContent.LocaleText> title,
                              List<LocalizedContent.LocaleText> description,
                              MediaAsset heroImage,
                              List<String> articleIds, List<String> inspirationIds,
                              List<String> spotlightIds, OffsetDateTime updatedAt) {
        this.id = id; this.slug = slug;
        this.titleTranslations = title; this.descriptionTranslations = description;
        this.heroImage = heroImage;
        this.articleIds = articleIds; this.inspirationIds = inspirationIds;
        this.spotlightIds = spotlightIds; this.updatedAt = updatedAt;
    }

    @Override public String getId() { return id; }
    public String getSlug() { return slug; }
    public LocalizedContent getTitle()       { return LocalizedContent.of(requestedLocale, "", titleTranslations); }
    public LocalizedContent getDescription() { return LocalizedContent.of(requestedLocale, "", descriptionTranslations); }
    public MediaAsset getHeroImage() { return heroImage; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public List<String> getArticleIds() { return articleIds; }
    public List<String> getInspirationIds() { return inspirationIds; }
    public List<String> getSpotlightIds() { return spotlightIds; }
    public String getRequestedLocale() { return requestedLocale; }

    public ContentCollection forLocale(String locale) {
        if (locale != null) this.requestedLocale = locale;
        return this;
    }
}
