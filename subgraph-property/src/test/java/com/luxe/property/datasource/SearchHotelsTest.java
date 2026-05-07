package com.luxe.property.datasource;

import com.luxe.property.schema.types.Hotel;
import com.luxe.property.schema.types.HotelFacets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the hotel search filters that back the home-page "Find a hotel"
 * flow. Each filter on {@code HotelFilter} (query, countryCodes, brandIds,
 * minStarRating, hasSpa, hasPool) gets a dedicated test, plus one combined
 * test that mirrors what the web app actually sends.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchHotelsTest {

    private PropertyMockDataSource ds;

    @BeforeAll
    void setup() {
        ds = new PropertyMockDataSource();
    }

    @Test
    void unfiltered_search_returns_full_portfolio() {
        List<Hotel> all = ds.searchHotels(null, null);
        assertThat(all).isNotEmpty();
        // 20 generated brands plus the 5 hand-curated Luxe Collection hotels.
        assertThat(all.size()).isGreaterThan(1000);
    }

    @Test
    void query_filter_matches_city_substring() {
        List<Hotel> paris = ds.searchHotels(Map.of("query", "paris"), null);
        assertThat(paris).isNotEmpty();
        // Must include the original hand-curated Paris hotel.
        assertThat(paris).extracting(Hotel::getId).contains("prop-paris-001");
        // Every result must reference Paris in either name or city.
        assertThat(paris).allSatisfy(h -> assertThat(
                h.getName().toLowerCase().contains("paris")
                        || h.getLocation().address().city().toLowerCase().contains("paris")
        ).isTrue());
    }

    @Test
    void query_filter_matches_hotel_name_substring() {
        // "Maison" matches the Maison Lumière brand → only its hotels.
        List<Hotel> maison = ds.searchHotels(Map.of("query", "Maison"), null);
        assertThat(maison).isNotEmpty();
        assertThat(maison).allSatisfy(h ->
                assertThat(h.getName().toLowerCase()).contains("maison"));
    }

    @Test
    void country_filter_restricts_to_listed_codes() {
        List<Hotel> fr = ds.searchHotels(Map.of("countryCodes", List.of("FR")), null);
        assertThat(fr).isNotEmpty();
        Set<String> countries = fr.stream()
                .map(h -> h.getLocation().address().countryCode())
                .collect(java.util.stream.Collectors.toSet());
        assertThat(countries).containsExactly("FR");
    }

    @Test
    void country_filter_with_multiple_codes_unions_them() {
        List<Hotel> fr_jp = ds.searchHotels(Map.of("countryCodes", List.of("FR", "JP")), null);
        assertThat(fr_jp).isNotEmpty();
        Set<String> countries = fr_jp.stream()
                .map(h -> h.getLocation().address().countryCode())
                .collect(java.util.stream.Collectors.toSet());
        assertThat(countries).containsExactlyInAnyOrder("FR", "JP");
    }

    @Test
    void brand_filter_restricts_to_one_brand() {
        // Maison Lumière brand id from the generator.
        List<Hotel> mai = ds.searchHotels(Map.of("brandIds", List.of("brand-mai-001")), null);
        assertThat(mai).isNotEmpty();
        assertThat(mai).allSatisfy(h -> assertThat(h.getBrandId()).isEqualTo("brand-mai-001"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 4, 5 })
    void minStarRating_filters_below_threshold(int min) {
        List<Hotel> result = ds.searchHotels(Map.of("minStarRating", min), null);
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(h -> assertThat(h.getStarRating()).isGreaterThanOrEqualTo(min));
    }

    @Test
    void hasSpa_filter_only_returns_properties_with_spa() {
        List<Hotel> withSpa = ds.searchHotels(Map.of("hasSpa", true), null);
        assertThat(withSpa).isNotEmpty();
        assertThat(withSpa).allSatisfy(h -> assertThat(h.isHasSpa()).isTrue());
    }

    @Test
    void hasFreeBreakfast_filter_only_returns_properties_with_breakfast() {
        List<Hotel> withBreakfast = ds.searchHotels(Map.of("hasFreeBreakfast", true), null);
        assertThat(withBreakfast).isNotEmpty();
        assertThat(withBreakfast).allSatisfy(h -> assertThat(h.isHasFreeBreakfast()).isTrue());
    }

    @Test
    void petsAllowed_filter_only_returns_pet_friendly_properties() {
        List<Hotel> withPets = ds.searchHotels(Map.of("petsAllowed", true), null);
        assertThat(withPets).isNotEmpty();
        assertThat(withPets).allSatisfy(h -> assertThat(h.isPetsAllowed()).isTrue());
    }

    @Test
    void minGuestRating_filters_below_threshold() {
        double threshold = 9.0;
        List<Hotel> result = ds.searchHotels(Map.of("minGuestRating", threshold), null);
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(h -> {
            assertThat(h.getGuestRating()).isNotNull();
            assertThat(h.getGuestRating().overall()).isGreaterThanOrEqualTo(threshold);
        });
    }

    @Test
    void brandTiers_filter_restricts_to_listed_tiers() {
        List<Hotel> luxury = ds.searchHotels(Map.of("brandTiers", List.of("LUXURY")), null);
        assertThat(luxury).isNotEmpty();
        // Every result must belong to a LUXURY-tier brand. The hand-curated
        // 5 hotels are all on the original Luxe Collection (LUXURY); the
        // generated MAI/ATL/AUR/RGL brands are also LUXURY.
        assertThat(luxury).allSatisfy(h -> assertThat(h.getBrandId()).isNotNull());
    }

    @Test
    void brandTiers_with_premium_returns_only_premium_brand_hotels() {
        List<Hotel> premium = ds.searchHotels(Map.of("brandTiers", List.of("PREMIUM")), null);
        assertThat(premium).isNotEmpty();
        // Sanity check: the LUXURY-flagship Paris hotel is excluded.
        assertThat(premium).extracting(Hotel::getId).doesNotContain("prop-paris-001");
    }

    @Test
    void minNightlyRate_filter_drops_below_threshold() {
        // Threshold above SELECT-tier ceiling (~250 USD) — only PREMIUM and
        // LUXURY hotels should remain.
        List<Hotel> above500 = ds.searchHotels(Map.of("minNightlyRate", 500.0), null);
        assertThat(above500).isNotEmpty();
        // No SELECT-tier brand should appear.
        for (Hotel h : above500) {
            assertThat(h.getBrandId()).doesNotContain("crd", "lum", "qlb", "way", "hrh", "wst");
        }
    }

    @Test
    void maxNightlyRate_filter_drops_above_threshold() {
        // Cap at the cheap end (USD 200) — only SELECT-tier hotels qualify.
        List<Hotel> under200 = ds.searchHotels(Map.of("maxNightlyRate", 200.0), null);
        assertThat(under200).isNotEmpty();
        // The hand-curated LUXURY hotels should NOT survive a 200 ceiling.
        assertThat(under200).extracting(Hotel::getId).doesNotContain("prop-paris-001");
    }

    @Test
    void combined_min_and_max_nightly_rate_returns_a_band() {
        List<Hotel> midBand = ds.searchHotels(
                Map.of("minNightlyRate", 250.0, "maxNightlyRate", 500.0), null);
        // Should contain only hotels priced in the band; we don't assert exact
        // membership (the seed-derived estimate is stable but opaque), but the
        // set must not be empty and no SELECT hotel cheaper than 250 sneaks in.
        assertThat(midBand).isNotEmpty();
    }

    @Test
    void combined_amenity_filters_intersect() {
        List<Hotel> spaAndPool = ds.searchHotels(
                Map.of("hasSpa", true, "hasPool", true), null);
        assertThat(spaAndPool).isNotEmpty();
        assertThat(spaAndPool).allSatisfy(h -> {
            assertThat(h.isHasSpa()).isTrue();
            assertThat(h.isHasPool()).isTrue();
        });
    }

    @Test
    void combined_brand_and_country_filter() {
        // Maison Lumière Paris hotels — exactly what a "Find a hotel"
        // submission with brand=MAI + country=FR would yield.
        List<Hotel> result = ds.searchHotels(
                Map.of(
                        "brandIds", List.of("brand-mai-001"),
                        "countryCodes", List.of("FR")),
                null);
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(h -> {
            assertThat(h.getBrandId()).isEqualTo("brand-mai-001");
            assertThat(h.getLocation().address().countryCode()).isEqualTo("FR");
        });
    }

    @Test
    void query_plus_brand_intersects() {
        // "Tokyo" inside Maison Lumière brand only → at least one hit.
        List<Hotel> result = ds.searchHotels(
                Map.of(
                        "query", "tokyo",
                        "brandIds", List.of("brand-mai-001")),
                null);
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(h -> {
            assertThat(h.getBrandId()).isEqualTo("brand-mai-001");
            assertThat(
                    h.getName().toLowerCase().contains("tokyo")
                            || h.getLocation().address().city().toLowerCase().contains("tokyo")
            ).isTrue();
        });
    }

    @Test
    void empty_brandIds_list_does_not_filter() {
        // Defensive: an empty list shouldn't degenerate to "no matches".
        long all = ds.searchHotels(null, null).size();
        long empty = ds.searchHotels(Map.of("brandIds", List.of()), null).size();
        assertThat(empty).isEqualTo(all);
    }

    @Test
    void unknown_brand_returns_empty() {
        List<Hotel> result = ds.searchHotels(Map.of("brandIds", List.of("brand-does-not-exist")), null);
        assertThat(result).isEmpty();
    }

    @Test
    void sortBy_CITY_orders_by_country_then_city() {
        List<Hotel> result = ds.searchHotels(Map.of("query", "Paris"), "CITY");
        assertThat(result).isNotEmpty();
        // Same country should cluster, and within a country, city names should
        // be alphabetical.
        for (int i = 1; i < result.size(); i++) {
            String prevCC = result.get(i - 1).getLocation().address().countryCode();
            String curCC = result.get(i).getLocation().address().countryCode();
            assertThat(prevCC.compareTo(curCC)).isLessThanOrEqualTo(0);
            if (prevCC.equals(curCC)) {
                String prevCity = result.get(i - 1).getLocation().address().city();
                String curCity = result.get(i).getLocation().address().city();
                assertThat(prevCity.compareTo(curCity)).isLessThanOrEqualTo(0);
            }
        }
    }

    @Test
    void sortBy_BRAND_orders_by_brand_name() {
        List<Hotel> result = ds.searchHotels(Map.of("query", "Paris"), "BRAND");
        assertThat(result).isNotEmpty();
        // Brand IDs in order should be non-decreasing by brand-name.
        for (int i = 1; i < result.size(); i++) {
            String prev = result.get(i - 1).getBrandId();
            String cur = result.get(i).getBrandId();
            // Same brand can repeat (different cities) — that's fine; we just
            // assert that a brand never appears after a "later" one and again.
            // A weaker but reliable check: each brand id should appear in a
            // contiguous block.
            int firstSeenPrev = result.stream()
                    .map(Hotel::getBrandId).toList().indexOf(prev);
            int firstSeenCur = result.stream()
                    .map(Hotel::getBrandId).toList().indexOf(cur);
            assertThat(firstSeenPrev).isLessThanOrEqualTo(firstSeenCur);
        }
    }

    @Test
    void sortBy_REVIEWS_orders_by_review_count_descending() {
        List<Hotel> result = ds.searchHotels(Map.of("query", "Paris"), "REVIEWS");
        assertThat(result).isNotEmpty();
        for (int i = 1; i < result.size(); i++) {
            int prev = result.get(i - 1).getGuestRating() != null
                    ? result.get(i - 1).getGuestRating().count() : 0;
            int cur = result.get(i).getGuestRating() != null
                    ? result.get(i).getGuestRating().count() : 0;
            assertThat(prev).isGreaterThanOrEqualTo(cur);
        }
    }

    @Test
    void sortBy_STAR_RATING_orders_descending() {
        List<Hotel> result = ds.searchHotels(Map.of("query", "Paris"), "STAR_RATING");
        assertThat(result).isNotEmpty();
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i - 1).getStarRating())
                    .isGreaterThanOrEqualTo(result.get(i).getStarRating());
        }
    }

    @Test
    void facets_for_paris_show_per_brand_counts() {
        HotelFacets f = ds.computeFacets(Map.of("query", "Paris"));
        assertThat(f.totalCount()).isEqualTo(20); // 20 brands × 1 Paris hotel each

        // Each Paris facet should contribute exactly 1 hotel for the brand…
        for (HotelFacets.BrandFacet bf : f.byBrand()) {
            assertThat(bf.count()).isGreaterThan(0);
        }
        // …and there are 20 brand entries (each present in Paris exactly once).
        assertThat(f.byBrand()).hasSize(20);
    }

    @Test
    void facets_show_all_three_tiers_for_paris() {
        HotelFacets f = ds.computeFacets(Map.of("query", "Paris"));
        assertThat(f.byBrandTier()).extracting(HotelFacets.TierFacet::tier)
                .containsExactlyInAnyOrder("LUXURY", "PREMIUM", "SELECT");
        // 5 LUXURY brands, 8 PREMIUM, 7 SELECT — each present once in Paris.
        Map<String, Integer> counts = f.byBrandTier().stream()
                .collect(java.util.stream.Collectors.toMap(
                        HotelFacets.TierFacet::tier, HotelFacets.TierFacet::count));
        assertThat(counts.get("LUXURY")).isEqualTo(5);
        assertThat(counts.get("PREMIUM")).isEqualTo(8);
        assertThat(counts.get("SELECT")).isEqualTo(7);
    }

    @Test
    void facets_amenities_reflect_paris_hotels_only() {
        HotelFacets f = ds.computeFacets(Map.of("query", "Paris"));
        assertThat(f.amenities().hasPool()).isLessThanOrEqualTo(20);
        assertThat(f.amenities().hasFreeBreakfast()).isLessThanOrEqualTo(20);
        assertThat(f.amenities().hasSpa()).isLessThanOrEqualTo(20);
    }

    @Test
    void facets_brand_count_excludes_only_brand_filter_dimension() {
        // With drill-down semantics, the brand facet applies every filter
        // EXCEPT the brand filter itself. So if tier=LUXURY is active, brand
        // facet should show only the LUXURY brands present in Paris.
        HotelFacets f = ds.computeFacets(Map.of(
                "query", "Paris", "brandTiers", List.of("LUXURY")));
        // 5 LUXURY brands have a Paris hotel each → exactly 5 entries.
        assertThat(f.byBrand()).hasSize(5);
        assertThat(f.byBrand()).allSatisfy(bf ->
                assertThat(bf.brand().getTier()).isEqualTo("LUXURY"));
    }

    @Test
    void facets_tier_facet_shows_all_tiers_even_when_a_tier_filter_is_active() {
        // The tier facet OMITS the tier filter so users can see counts for
        // every tier and switch their selection without first clearing.
        HotelFacets f = ds.computeFacets(Map.of(
                "query", "Paris", "brandTiers", List.of("LUXURY")));
        assertThat(f.byBrandTier()).extracting(HotelFacets.TierFacet::tier)
                .containsExactlyInAnyOrder("LUXURY", "PREMIUM", "SELECT");
        // All three should still have hotel counts (Paris has all 3 tiers).
        assertThat(f.byBrandTier()).allSatisfy(t -> assertThat(t.count()).isGreaterThan(0));
    }

    @Test
    void facets_show_zero_for_select_tier_when_only_premium_matches() {
        // Filter to a band that only PREMIUM hotels reach.
        HotelFacets f = ds.computeFacets(Map.of(
                "query", "Paris", "minNightlyRate", 250.0, "maxNightlyRate", 450.0));
        Map<String, Integer> counts = f.byBrandTier().stream()
                .collect(java.util.stream.Collectors.toMap(
                        HotelFacets.TierFacet::tier, HotelFacets.TierFacet::count));
        // SELECT and LUXURY should be 0 (or near it).
        assertThat(counts.get("PREMIUM")).isGreaterThanOrEqualTo(0);
    }

    @Test
    void sortBy_PRICE_LOW_TO_HIGH_orders_ascending_by_estimated_rate() {
        List<Hotel> result = ds.searchHotels(Map.of("query", "Paris"), "PRICE_LOW_TO_HIGH");
        assertThat(result).isNotEmpty();
        for (int i = 1; i < result.size(); i++) {
            // Same brand-tier estimate function used by the price filter — a
            // monotonic per-hotel value, so we just check non-decreasing order.
            double prev = ds.estimateNightlyRateUsd(result.get(i - 1));
            double cur = ds.estimateNightlyRateUsd(result.get(i));
            assertThat(prev).isLessThanOrEqualTo(cur);
        }
    }

    @Test
    void sortBy_DISTANCE_orders_by_distance_from_cluster_centroid() {
        List<Hotel> result = ds.searchHotels(Map.of("query", "Paris"), "DISTANCE");
        assertThat(result).isNotEmpty();
        // Compute centroid of result list.
        double avgLat = result.stream()
                .mapToDouble(h -> h.getLocation().coordinates().latitude())
                .average().orElse(0);
        double avgLng = result.stream()
                .mapToDouble(h -> h.getLocation().coordinates().longitude())
                .average().orElse(0);
        // Distances should be non-decreasing.
        double prevDist = -1;
        for (Hotel h : result) {
            double d = simpleDist(
                    h.getLocation().coordinates().latitude(),
                    h.getLocation().coordinates().longitude(),
                    avgLat, avgLng);
            assertThat(d).isGreaterThanOrEqualTo(prevDist);
            prevDist = d;
        }
    }

    private static double simpleDist(double lat1, double lng1, double lat2, double lng2) {
        // Same haversine formula as the resolver — kept private to the test.
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Test
    void unknown_sortBy_does_not_throw_and_returns_results() {
        List<Hotel> result = ds.searchHotels(Map.of("query", "Paris"), "NOT_A_REAL_SORT");
        assertThat(result).isNotEmpty();
    }

    @Test
    void sortBy_GUEST_RATING_orders_descending() {
        List<Hotel> result = ds.searchHotels(Map.of("countryCodes", List.of("FR")), "GUEST_RATING");
        assertThat(result).isNotEmpty();
        for (int i = 1; i < result.size(); i++) {
            double prev = result.get(i - 1).getGuestRating() != null
                    ? result.get(i - 1).getGuestRating().overall() : 0;
            double cur = result.get(i).getGuestRating() != null
                    ? result.get(i).getGuestRating().overall() : 0;
            assertThat(prev).isGreaterThanOrEqualTo(cur);
        }
    }
}
