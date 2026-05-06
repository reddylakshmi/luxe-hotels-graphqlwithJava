package com.luxe.content.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DealSpotlight implements HasId {
    private final String id, slug, status, promoCode, ctaUrl;
    private final List<LocalizedContent.LocaleText> titleTranslations;
    private final List<LocalizedContent.LocaleText> descriptionTranslations;
    private final List<LocalizedContent.LocaleText> termsTranslations;
    private final List<LocalizedContent.LocaleText> ctaLabelTranslations;
    private final MediaAsset heroImage;
    private final Double discountPercent;
    private final LocalDate validFrom, validTo;
    private final List<String> applicableHotelIds;
    private String requestedLocale = "en";

    public DealSpotlight(String id, String slug, String status, String promoCode,
                         Double discountPercent, LocalDate validFrom, LocalDate validTo,
                         List<String> applicableHotelIds, String ctaUrl,
                         List<LocalizedContent.LocaleText> title,
                         List<LocalizedContent.LocaleText> description,
                         List<LocalizedContent.LocaleText> terms,
                         List<LocalizedContent.LocaleText> ctaLabel,
                         MediaAsset heroImage) {
        this.id = id; this.slug = slug; this.status = status;
        this.promoCode = promoCode; this.discountPercent = discountPercent;
        this.validFrom = validFrom; this.validTo = validTo;
        this.applicableHotelIds = applicableHotelIds;
        this.ctaUrl = ctaUrl;
        this.titleTranslations = title; this.descriptionTranslations = description;
        this.termsTranslations = terms; this.ctaLabelTranslations = ctaLabel;
        this.heroImage = heroImage;
    }

    @Override public String getId() { return id; }
    public String getSlug() { return slug; }
    public String getStatus() { return status; }
    public String getPromoCode() { return promoCode; }
    public Double getDiscountPercent() { return discountPercent; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public MediaAsset getHeroImage() { return heroImage; }
    public String getCtaUrl() { return ctaUrl; }

    public LocalizedContent getTitle()             { return LocalizedContent.of(requestedLocale, "", titleTranslations); }
    public LocalizedContent getDescription()       { return LocalizedContent.of(requestedLocale, "", descriptionTranslations); }
    public LocalizedContent getTermsAndConditions(){ return LocalizedContent.of(requestedLocale, "", termsTranslations); }
    public LocalizedContent getCtaLabel()          { return LocalizedContent.of(requestedLocale, "", ctaLabelTranslations); }
    public List<Map<String, Object>> getApplicableHotels() {
        return applicableHotelIds.stream().<Map<String, Object>>map(id -> Map.of("id", id)).toList();
    }

    public DealSpotlight forLocale(String locale) {
        if (locale != null) this.requestedLocale = locale;
        return this;
    }
}
