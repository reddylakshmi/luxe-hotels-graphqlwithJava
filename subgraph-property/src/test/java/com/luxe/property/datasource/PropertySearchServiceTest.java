package com.luxe.property.datasource;

import com.luxe.property.schema.types.Hotel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused tests for the extracted {@link PropertySearchService} — exercising
 * search/facets/autocomplete directly via the service rather than through
 * {@link PropertyMockDataSource}. Keeps the seam testable in isolation.
 */
class PropertySearchServiceTest {

    /**
     * Build a search service backed by the same mock inventory as the data
     * source. The shared seed data is the realistic universe to test against
     * (hand-curated + India + ~1500 generator hotels).
     */
    private PropertySearchService freshService() {
        PropertyMockDataSource ds = new PropertyMockDataSource();
        return new PropertySearchService(
                ds.searchHotels(null, null).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Hotel::getId, h -> h, (a, b) -> a, java.util.LinkedHashMap::new)),
                ds.getAllBrands(null).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.luxe.property.schema.types.Brand::getId, b -> b,
                                (a, b) -> a, java.util.LinkedHashMap::new)));
    }

    @Test
    void searchHotels_with_null_filter_returns_every_hotel_in_insertion_order() {
        PropertySearchService search = freshService();
        List<Hotel> all = search.searchHotels(null, null);
        assertThat(all).isNotEmpty();
        // Paris is the first hand-curated hotel inserted in PropertyMockDataSource
        // — confirms the service is iterating the underlying ordered map.
        assertThat(all.get(0).getId()).isEqualTo("prop-paris-001");
    }

    @Test
    void searchHotels_filters_destination_by_city_country_state() {
        PropertySearchService search = freshService();
        // City match
        assertThat(search.searchHotels(Map.of("query", "Paris"), null))
                .anySatisfy(h -> assertThat(h.getLocation().address().city()).isEqualToIgnoringCase("Paris"));
        // Country name (substring)
        assertThat(search.searchHotels(Map.of("query", "France"), null))
                .anySatisfy(h -> assertThat(h.getLocation().address().countryName()).isEqualToIgnoringCase("France"));
        // State (substring) — Hyderabad hotels are seeded with "Telangana"
        assertThat(search.searchHotels(Map.of("query", "Telangana"), null))
                .allSatisfy(h -> assertThat(h.getLocation().address().state())
                        .isEqualToIgnoringCase("Telangana"));
        // Country code (exact)
        assertThat(search.searchHotels(Map.of("query", "FR"), null))
                .extracting(h -> h.getLocation().address().countryCode())
                .contains("FR");
    }

    @Test
    void searchHotels_explicit_empty_ids_returns_nothing() {
        PropertySearchService search = freshService();
        assertThat(search.searchHotels(Map.of("ids", List.of()), null)).isEmpty();
    }

    @Test
    void searchHotels_with_known_ids_returns_only_those() {
        PropertySearchService search = freshService();
        List<String> wanted = List.of("prop-paris-001", "prop-india-bom-bkc");
        assertThat(search.searchHotels(Map.of("ids", wanted), null))
                .extracting(Hotel::getId)
                .containsExactlyInAnyOrderElementsOf(wanted);
    }

    @Test
    void destinationSuggestions_blank_query_returns_empty() {
        PropertySearchService search = freshService();
        assertThat(search.destinationSuggestions("", 10)).isEmpty();
        assertThat(search.destinationSuggestions(null, 10)).isEmpty();
        assertThat(search.destinationSuggestions("Paris", 0)).isEmpty();
    }

    @Test
    void destinationSuggestions_caps_at_limit() {
        PropertySearchService search = freshService();
        assertThat(search.destinationSuggestions("a", 5)).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    void computeFacets_returns_a_consistent_total_count() {
        PropertySearchService search = freshService();
        var facets = search.computeFacets(null);
        assertThat(facets).isNotNull();
        // Total count should match the unfiltered search size.
        assertThat(facets.totalCount()).isEqualTo(search.searchHotels(null, null).size());
    }

    @Test
    void computeFacets_respects_active_filter_outside_the_dimension() {
        PropertySearchService search = freshService();
        var facets = search.computeFacets(Map.of("query", "Paris"));
        assertThat(facets.totalCount()).isLessThan(search.searchHotels(null, null).size());
        assertThat(facets.byCity()).extracting(h -> h.city())
                .anyMatch(c -> c.equalsIgnoreCase("Paris"));
    }

    @Test
    void estimateNightlyRateUsd_buckets_by_brand_tier() {
        PropertySearchService search = freshService();
        // Hand-curated Paris hotel uses brand-lux-001 (LUXURY) → ≥ 400 USD
        Hotel paris = search.searchHotels(null, null).stream()
                .filter(h -> "prop-paris-001".equals(h.getId()))
                .findFirst().orElseThrow();
        assertThat(search.estimateNightlyRateUsd(paris)).isGreaterThanOrEqualTo(400);
    }
}
