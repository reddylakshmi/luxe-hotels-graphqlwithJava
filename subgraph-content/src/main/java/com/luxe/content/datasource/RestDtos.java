package com.luxe.content.datasource;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Wire-format DTOs that mirror the JSON shape served by luxe-content-api.
 * These are deliberately decoupled from {@code com.luxe.content.schema.types}
 * so the backend's contract can evolve independently from the GraphQL schema.
 * {@link ContentRestDataSource} maps DTOs into the domain types after fetching.
 */
final class RestDtos {
    private RestDtos() {}

    record LocaleTextDto(String locale, String value) {}

    record MediaAssetDto(String id, String url, String thumbnailUrl,
                          List<LocaleTextDto> alt, List<LocaleTextDto> caption,
                          String type, Integer width, Integer height, Integer durationSec) {}

    record AuthorDto(String id, String name, String role,
                      List<LocaleTextDto> bio, String avatarUrl) {}

    record ArticleDto(String id, String slug, String category, String status,
                       List<LocaleTextDto> title, List<LocaleTextDto> subtitle,
                       List<LocaleTextDto> body, List<LocaleTextDto> excerpt,
                       MediaAssetDto heroImage, List<MediaAssetDto> gallery,
                       AuthorDto author, List<String> tags, List<String> relatedHotelIds,
                       int readTimeMinutes,
                       OffsetDateTime publishedAt, OffsetDateTime updatedAt) {}

    record MoneyDto(String amount, String currency) {}

    record InspirationDto(String id, String slug, String destination, String region,
                           String bestSeason,
                           List<LocaleTextDto> title, List<LocaleTextDto> story,
                           MediaAssetDto heroImage, List<MediaAssetDto> gallery,
                           List<List<LocaleTextDto>> highlights,
                           List<String> suggestedHotelIds,
                           MoneyDto fromPriceUsd, int durationDays,
                           OffsetDateTime updatedAt) {}

    record BrandPillarDto(String code, String iconKey,
                           List<LocaleTextDto> title, List<LocaleTextDto> body) {}

    record BrandStoryDto(String id,
                          List<LocaleTextDto> headline, List<LocaleTextDto> subheadline,
                          List<LocaleTextDto> body,
                          List<BrandPillarDto> pillars,
                          MediaAssetDto heroImage, String filmUrl,
                          OffsetDateTime updatedAt) {}

    record DealSpotlightDto(String id, String slug, String status,
                              String promoCode, Double discountPercent,
                              LocalDate validFrom, LocalDate validTo,
                              List<String> applicableHotelIds, String ctaUrl,
                              List<LocaleTextDto> title, List<LocaleTextDto> body,
                              List<LocaleTextDto> terms, List<LocaleTextDto> cta,
                              MediaAssetDto heroImage) {}

    record CollectionDto(String id, String slug,
                          List<LocaleTextDto> title, List<LocaleTextDto> description,
                          MediaAssetDto heroImage,
                          List<String> articleIds, List<String> inspirationIds,
                          List<String> spotlightIds, OffsetDateTime updatedAt) {}
}
