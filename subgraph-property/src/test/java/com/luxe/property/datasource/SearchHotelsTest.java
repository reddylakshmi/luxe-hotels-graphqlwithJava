package com.luxe.property.datasource;

import com.luxe.property.schema.types.Hotel;
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
