package com.luxe.content.schema.types;

import java.time.OffsetDateTime;
import java.util.List;

public class BrandStory {
    private final String id;
    private final List<LocalizedContent.LocaleText> titleTranslations;
    private final List<LocalizedContent.LocaleText> taglineTranslations;
    private final List<LocalizedContent.LocaleText> bodyTranslations;
    private final List<BrandPillar> pillars;
    private final MediaAsset heroImage;
    private final String videoUrl;
    private final OffsetDateTime updatedAt;
    private String requestedLocale = "en";

    public BrandStory(String id,
                       List<LocalizedContent.LocaleText> title,
                       List<LocalizedContent.LocaleText> tagline,
                       List<LocalizedContent.LocaleText> body,
                       List<BrandPillar> pillars, MediaAsset heroImage, String videoUrl,
                       OffsetDateTime updatedAt) {
        this.id = id; this.titleTranslations = title; this.taglineTranslations = tagline;
        this.bodyTranslations = body; this.pillars = pillars; this.heroImage = heroImage;
        this.videoUrl = videoUrl; this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public LocalizedContent getTitle()   { return LocalizedContent.of(requestedLocale, "", titleTranslations); }
    public LocalizedContent getTagline() { return LocalizedContent.of(requestedLocale, "", taglineTranslations); }
    public LocalizedContent getBody()    { return LocalizedContent.of(requestedLocale, "", bodyTranslations); }
    public List<BrandPillar> getPillars() {
        return pillars.stream().map(p -> p.forLocale(requestedLocale)).toList();
    }
    public MediaAsset getHeroImage() { return heroImage; }
    public String getVideoUrl() { return videoUrl; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public BrandStory forLocale(String locale) {
        if (locale != null) this.requestedLocale = locale;
        return this;
    }

    public static class BrandPillar {
        private final String code, icon;
        private final List<LocalizedContent.LocaleText> titleTranslations;
        private final List<LocalizedContent.LocaleText> descriptionTranslations;
        private String requestedLocale = "en";

        public BrandPillar(String code, String icon,
                            List<LocalizedContent.LocaleText> title,
                            List<LocalizedContent.LocaleText> description) {
            this.code = code; this.icon = icon;
            this.titleTranslations = title; this.descriptionTranslations = description;
        }

        public String getCode() { return code; }
        public String getIcon() { return icon; }
        public LocalizedContent getTitle()       { return LocalizedContent.of(requestedLocale, "", titleTranslations); }
        public LocalizedContent getDescription() { return LocalizedContent.of(requestedLocale, "", descriptionTranslations); }

        public BrandPillar forLocale(String locale) {
            if (locale != null) this.requestedLocale = locale;
            return this;
        }
    }
}
