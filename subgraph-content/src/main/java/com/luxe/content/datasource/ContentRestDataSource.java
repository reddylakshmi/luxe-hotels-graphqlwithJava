package com.luxe.content.datasource;

import com.luxe.common.config.LuxeBackendProperties;
import com.luxe.common.scalar.Money;
import com.luxe.content.schema.types.*;
import com.luxe.content.schema.types.LocalizedContent.LocaleText;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.*;

/**
 * REST-backed implementation of {@link ContentDataSource}. Loaded only when
 * {@code luxe.backend.base-url} is set to a non-empty value. The mock impl
 * is loaded otherwise (see {@link ContentMockDataSource}).
 *
 * <p>The backend (luxe-hotels-content-api) returns flat JSON DTOs; we map them into
 * the subgraph's existing domain types so the resolver layer stays
 * unchanged.
 */
@Component
@ConditionalOnExpression("'${luxe.backend.base-url:}'.length() > 0")
public class ContentRestDataSource implements ContentDataSource {

    private final RestClient http;

    public ContentRestDataSource(LuxeBackendProperties props) {
        this.http = RestClient.builder().baseUrl(props.getBaseUrl()).build();
    }

    // ── Articles ─────────────────────────────────────────────────────────────

    @Override
    public List<Article> findArticles(Map<String, Object> filter, String locale) {
        String category = filter != null ? (String) filter.get("category") : null;
        String tag = filter != null ? (String) filter.get("tag") : null;
        String hotelId = filter != null ? (String) filter.get("hotelId") : null;
        List<RestDtos.ArticleDto> dtos = http.get()
                .uri(uri -> uri.path("/api/content/articles")
                        .queryParamIfPresent("category", Optional.ofNullable(category))
                        .queryParamIfPresent("tag", Optional.ofNullable(tag))
                        .queryParamIfPresent("hotelId", Optional.ofNullable(hotelId))
                        .build())
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return dtos == null ? List.of() : dtos.stream().map(d -> toArticle(d, locale)).toList();
    }

    @Override
    public Optional<Article> findArticleBySlug(String slug, String locale) {
        return Optional.ofNullable(http.get().uri("/api/content/articles/by-slug/{slug}", slug)
                        .retrieve().body(RestDtos.ArticleDto.class))
                .map(d -> toArticle(d, locale));
    }

    @Override
    public List<Article> findFeaturedArticles(int limit, String locale) {
        List<RestDtos.ArticleDto> dtos = http.get()
                .uri(uri -> uri.path("/api/content/articles/featured")
                        .queryParam("limit", limit).build())
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return dtos == null ? List.of() : dtos.stream().map(d -> toArticle(d, locale)).toList();
    }

    @Override
    public Optional<Article> findArticleById(String id) {
        return Optional.ofNullable(http.get().uri("/api/content/articles/{id}", id)
                        .retrieve().body(RestDtos.ArticleDto.class))
                .map(d -> toArticle(d, "en"));
    }

    @Override
    public List<Article> articlesByIds(List<String> ids, String locale) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<RestDtos.ArticleDto> dtos = http.post().uri("/api/content/articles/by-ids")
                .body(ids).retrieve().body(new ParameterizedTypeReference<>() {});
        return dtos == null ? List.of() : dtos.stream().map(d -> toArticle(d, locale)).toList();
    }

    // ── Inspirations ─────────────────────────────────────────────────────────

    @Override
    public List<TravelInspiration> findInspirations(String destination, String season,
                                                      int limit, String locale) {
        List<RestDtos.InspirationDto> dtos = http.get()
                .uri(uri -> uri.path("/api/content/inspirations")
                        .queryParamIfPresent("destination", Optional.ofNullable(destination))
                        .queryParamIfPresent("season", Optional.ofNullable(season))
                        .queryParam("limit", limit).build())
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return dtos == null ? List.of() : dtos.stream().map(d -> toInspiration(d, locale)).toList();
    }

    @Override
    public List<TravelInspiration> inspirationsByIds(List<String> ids, String locale) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<RestDtos.InspirationDto> dtos = http.post().uri("/api/content/inspirations/by-ids")
                .body(ids).retrieve().body(new ParameterizedTypeReference<>() {});
        return dtos == null ? List.of() : dtos.stream().map(d -> toInspiration(d, locale)).toList();
    }

    // ── Brand story ──────────────────────────────────────────────────────────

    @Override
    public BrandStory getBrandStory(String locale) {
        RestDtos.BrandStoryDto dto = http.get().uri("/api/content/brand-story")
                .retrieve().body(RestDtos.BrandStoryDto.class);
        return dto == null ? null : toBrandStory(dto, locale);
    }

    // ── Spotlights ───────────────────────────────────────────────────────────

    @Override
    public List<DealSpotlight> findDealSpotlights(Boolean active, String locale) {
        List<RestDtos.DealSpotlightDto> dtos = http.get()
                .uri(uri -> uri.path("/api/content/deal-spotlights")
                        .queryParamIfPresent("active", Optional.ofNullable(active))
                        .build())
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return dtos == null ? List.of() : dtos.stream().map(d -> toSpotlight(d, locale)).toList();
    }

    @Override
    public List<DealSpotlight> spotlightsByIds(List<String> ids, String locale) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<RestDtos.DealSpotlightDto> dtos = http.post().uri("/api/content/spotlights/by-ids")
                .body(ids).retrieve().body(new ParameterizedTypeReference<>() {});
        return dtos == null ? List.of() : dtos.stream().map(d -> toSpotlight(d, locale)).toList();
    }

    // ── Collections ──────────────────────────────────────────────────────────

    @Override
    public Optional<ContentCollection> findCollectionBySlug(String slug, String locale) {
        return Optional.ofNullable(http.get().uri("/api/content/collections/by-slug/{slug}", slug)
                        .retrieve().body(RestDtos.CollectionDto.class))
                .map(d -> toCollection(d, locale));
    }

    // ── DTO → domain mappers ─────────────────────────────────────────────────

    private static List<LocaleText> toLocaleTexts(List<RestDtos.LocaleTextDto> in) {
        if (in == null) return List.of();
        return in.stream().map(t -> new LocaleText(t.locale(), t.value())).toList();
    }

    private static LocalizedContent toLocalized(List<RestDtos.LocaleTextDto> in, String locale) {
        if (in == null || in.isEmpty()) return LocalizedContent.ofEnglish("");
        return LocalizedContent.of(locale != null ? locale : "en", "", toLocaleTexts(in));
    }

    private static MediaAsset toMedia(RestDtos.MediaAssetDto m) {
        if (m == null) return null;
        return new MediaAsset(m.id(), m.url(), m.thumbnailUrl(),
                m.alt() == null || m.alt().isEmpty() ? null : LocalizedContent.of("en", "", toLocaleTexts(m.alt())),
                m.caption() == null || m.caption().isEmpty() ? null : LocalizedContent.of("en", "", toLocaleTexts(m.caption())),
                m.type(), m.width(), m.height(), m.durationSec());
    }

    private static List<MediaAsset> toMediaList(List<RestDtos.MediaAssetDto> in) {
        if (in == null) return List.of();
        return in.stream().map(ContentRestDataSource::toMedia).toList();
    }

    private static ContentAuthor toAuthor(RestDtos.AuthorDto a) {
        if (a == null) return null;
        return new ContentAuthor(a.id(), a.name(), a.role(),
                a.bio() == null || a.bio().isEmpty() ? null : LocalizedContent.of("en", "", toLocaleTexts(a.bio())),
                a.avatarUrl());
    }

    private static Article toArticle(RestDtos.ArticleDto d, String locale) {
        Article a = new Article(d.id(), d.slug(), d.category(), d.status(),
                toLocaleTexts(d.title()), toLocaleTexts(d.subtitle()),
                toLocaleTexts(d.body()), toLocaleTexts(d.excerpt()),
                toMedia(d.heroImage()), toMediaList(d.gallery()),
                toAuthor(d.author()),
                d.tags() != null ? d.tags() : List.of(),
                d.relatedHotelIds() != null ? d.relatedHotelIds() : List.of(),
                d.readTimeMinutes(), d.publishedAt(), d.updatedAt());
        return a.forLocale(locale);
    }

    private static TravelInspiration toInspiration(RestDtos.InspirationDto d, String locale) {
        Money budget = d.fromPriceUsd() == null ? null
                : Money.of(new BigDecimal(d.fromPriceUsd().amount()), d.fromPriceUsd().currency());
        List<List<LocaleText>> highlights = d.highlights() == null ? List.of()
                : d.highlights().stream().map(ContentRestDataSource::toLocaleTexts).toList();
        TravelInspiration t = new TravelInspiration(d.id(), d.slug(), d.destination(),
                d.region(), d.bestSeason(),
                toLocaleTexts(d.title()), toLocaleTexts(d.story()),
                toMedia(d.heroImage()), toMediaList(d.gallery()),
                highlights,
                d.suggestedHotelIds() != null ? d.suggestedHotelIds() : List.of(),
                budget, d.durationDays(), d.updatedAt());
        return t.forLocale(locale);
    }

    private static BrandStory toBrandStory(RestDtos.BrandStoryDto d, String locale) {
        List<BrandStory.BrandPillar> pillars = d.pillars() == null ? List.of()
                : d.pillars().stream().map(p ->
                        new BrandStory.BrandPillar(p.code(), p.iconKey(),
                                toLocaleTexts(p.title()), toLocaleTexts(p.body())))
                    .toList();
        BrandStory b = new BrandStory(d.id(),
                toLocaleTexts(d.headline()), toLocaleTexts(d.subheadline()),
                toLocaleTexts(d.body()),
                pillars, toMedia(d.heroImage()), d.filmUrl(), d.updatedAt());
        return b.forLocale(locale);
    }

    private static DealSpotlight toSpotlight(RestDtos.DealSpotlightDto d, String locale) {
        DealSpotlight s = new DealSpotlight(d.id(), d.slug(), d.status(),
                d.promoCode(), d.discountPercent(),
                d.validFrom(), d.validTo(),
                d.applicableHotelIds() != null ? d.applicableHotelIds() : List.of(),
                d.ctaUrl(),
                toLocaleTexts(d.title()), toLocaleTexts(d.body()),
                toLocaleTexts(d.terms()), toLocaleTexts(d.cta()),
                toMedia(d.heroImage()));
        return s.forLocale(locale);
    }

    private static ContentCollection toCollection(RestDtos.CollectionDto d, String locale) {
        ContentCollection c = new ContentCollection(d.id(), d.slug(),
                toLocaleTexts(d.title()), toLocaleTexts(d.description()),
                toMedia(d.heroImage()),
                d.articleIds() != null ? d.articleIds() : List.of(),
                d.inspirationIds() != null ? d.inspirationIds() : List.of(),
                d.spotlightIds() != null ? d.spotlightIds() : List.of(),
                d.updatedAt());
        return c.forLocale(locale);
    }
}
