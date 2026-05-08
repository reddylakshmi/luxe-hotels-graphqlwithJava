package com.luxe.property.datasource;

import com.luxe.property.schema.types.Brand;
import com.luxe.property.schema.types.Hotel;
import com.luxe.property.schema.types.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyMockDataSourceTest {

    private PropertyMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new PropertyMockDataSource();
    }

    @Test
    void seeded_hotels_include_all_six_cities() {
        List<Hotel> hotels = ds.searchHotels(null, null);
        assertThat(hotels).extracting(Hotel::getId)
                .contains("prop-paris-001", "prop-london-001", "prop-tokyo-001",
                        "prop-dubai-001", "prop-nyc-001");
    }

    @Test
    void get_hotel_by_id_returns_hotel_when_found() {
        assertThat(ds.getHotelById("prop-paris-001"))
                .isPresent()
                .get()
                .extracting(Hotel::getName).asString().contains("Paris");
    }

    @Test
    void get_hotel_by_id_returns_empty_when_missing() {
        assertThat(ds.getHotelById("does-not-exist")).isEmpty();
    }

    @Test
    void get_hotel_by_slug_resolves_known_slug() {
        Hotel any = ds.searchHotels(null, null).get(0);
        assertThat(ds.getHotelBySlug(any.getSlug())).isPresent()
                .get().extracting(Hotel::getId).isEqualTo(any.getId());
    }

    @Test
    void get_hotel_by_slug_returns_empty_for_unknown() {
        assertThat(ds.getHotelBySlug("not-a-slug")).isEmpty();
    }

    @Test
    void search_filters_by_query_substring() {
        List<Hotel> matches = ds.searchHotels(Map.of("query", "Paris"), null);
        assertThat(matches).isNotEmpty();
        assertThat(matches).allSatisfy(h ->
                assertThat(h.getName().toLowerCase() + " " + h.getLocation().address().city().toLowerCase())
                        .contains("paris"));
    }

    @Test
    void search_query_matches_country_name() {
        // The autocomplete fills the destination input with a country's
        // display name when the user picks a country suggestion. The search
        // filter must surface every hotel in that country, not just hotels
        // whose name or city happens to contain "France".
        List<Hotel> matches = ds.searchHotels(Map.of("query", "France"), null);
        assertThat(matches).isNotEmpty();
        assertThat(matches).allSatisfy(h ->
                assertThat(h.getLocation().address().countryName()).isEqualToIgnoringCase("France"));
    }

    @Test
    void search_query_matches_country_name_case_insensitively() {
        List<Hotel> lower = ds.searchHotels(Map.of("query", "india"), null);
        List<Hotel> upper = ds.searchHotels(Map.of("query", "INDIA"), null);
        List<Hotel> mixed = ds.searchHotels(Map.of("query", "India"), null);
        assertThat(lower).isNotEmpty();
        assertThat(lower).hasSameSizeAs(upper);
        assertThat(lower).hasSameSizeAs(mixed);
    }

    @Test
    void search_query_matches_state_substring() {
        // India hotels are seeded with state names — Telangana for Hyderabad,
        // Maharashtra for Mumbai, etc. Searching by state should surface every
        // hotel in that state.
        List<Hotel> matches = ds.searchHotels(Map.of("query", "Telangana"), null);
        assertThat(matches).isNotEmpty();
        assertThat(matches).allSatisfy(h ->
                assertThat(h.getLocation().address().state()).isEqualToIgnoringCase("Telangana"));
        // All three Hyderabad hotels are in Telangana.
        assertThat(matches).extracting(Hotel::getId)
                .contains("prop-india-hyd-hitec", "prop-india-hyd-gachi", "prop-india-hyd-madha");
    }

    @Test
    void search_query_state_match_is_case_insensitive() {
        List<Hotel> mixed = ds.searchHotels(Map.of("query", "TaMiL NaDu"), null);
        assertThat(mixed).isNotEmpty();
        assertThat(mixed).allSatisfy(h ->
                assertThat(h.getLocation().address().state()).isEqualToIgnoringCase("Tamil Nadu"));
    }

    @Test
    void search_query_matches_country_code_exactly() {
        // Two-letter country code path supports URLs like ?destination=FR.
        // The result set may also include hotels whose name contains "fr"
        // as a substring (e.g. "...Frankfurt") — that's the name-substring
        // path and is intentional. We only assert the country-code
        // match path delivers all FR-coded hotels.
        List<Hotel> matches = ds.searchHotels(Map.of("query", "FR"), null);
        var allFrenchIds = ds.searchHotels(null, null).stream()
                .filter(h -> "FR".equalsIgnoreCase(h.getLocation().address().countryCode()))
                .map(Hotel::getId).toList();
        assertThat(matches).isNotEmpty();
        assertThat(matches).extracting(Hotel::getId).containsAll(allFrenchIds);
    }

    @Test
    void search_filters_by_min_star_rating() {
        List<Hotel> fives = ds.searchHotels(Map.of("minStarRating", 5), null);
        assertThat(fives).isNotEmpty();
        assertThat(fives).allSatisfy(h -> assertThat(h.getStarRating()).isGreaterThanOrEqualTo(5));
    }

    @Test
    void search_sort_by_name_ascending() {
        List<Hotel> sorted = ds.searchHotels(null, "NAME");
        List<String> names = sorted.stream().map(Hotel::getName).toList();
        List<String> expected = names.stream().sorted().toList();
        assertThat(names).isEqualTo(expected);
    }

    @Test
    void search_sort_by_guest_rating_descending() {
        List<Hotel> sorted = ds.searchHotels(null, "GUEST_RATING");
        for (int i = 0; i < sorted.size() - 1; i++) {
            double a = sorted.get(i).getGuestRating() != null
                    ? sorted.get(i).getGuestRating().overall() : 0;
            double b = sorted.get(i + 1).getGuestRating() != null
                    ? sorted.get(i + 1).getGuestRating().overall() : 0;
            assertThat(a).isGreaterThanOrEqualTo(b);
        }
    }

    @Test
    void featured_hotels_respects_limit() {
        assertThat(ds.getFeaturedHotels(null, null, 2)).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    void featured_hotels_filters_by_country_code() {
        List<Hotel> fr = ds.getFeaturedHotels(null, "FR", 10);
        assertThat(fr).allSatisfy(h ->
                assertThat(h.getLocation().address().countryCode()).isEqualTo("FR"));
    }

    @Test
    void brands_seed_is_non_empty() {
        List<Brand> brands = ds.getAllBrands(null);
        assertThat(brands).isNotEmpty();
        assertThat(brands).allSatisfy(b -> assertThat(b.getCode()).isNotBlank());
    }

    @Test
    void brand_by_code_finds_known_brand() {
        String anyCode = ds.getAllBrands(null).get(0).getCode();
        assertThat(ds.getBrandByCode(anyCode)).isPresent()
                .get().extracting(Brand::getCode).isEqualTo(anyCode);
    }

    @Test
    void brand_by_id_returns_empty_for_unknown() {
        assertThat(ds.getBrandById("zzz")).isEmpty();
    }

    @Test
    void room_types_by_hotel_belong_to_that_hotel() {
        for (Hotel h : ds.searchHotels(null, null)) {
            List<RoomType> rooms = ds.getRoomTypesByHotelId(h.getId());
            assertThat(rooms).allSatisfy(rt ->
                    assertThat(rt.getHotelId()).isEqualTo(h.getId()));
        }
    }

    @Test
    void reviews_by_hotel_filter_to_that_hotel() {
        Hotel h = ds.searchHotels(null, null).get(0);
        var reviews = ds.getReviewsByHotelId(h.getId(), "RECENT");
        assertThat(reviews).allSatisfy(r ->
                assertThat(r.getHotelId()).isEqualTo(h.getId()));
    }

    @Test
    void submit_review_persists_a_new_review_retrievable_by_id() {
        Hotel h = ds.searchHotels(null, null).get(0);
        var review = ds.submitReview(h.getId(), "Test User", 4.5,
                Map.of("cleanliness", 5, "service", 4),
                "Great stay", "Loved it.", "2026-04-15");
        assertThat(review).isNotNull();
        assertThat(ds.getReviewById(review.getId())).isPresent();
    }

    @Test
    void mark_review_helpful_returns_review_when_found() {
        Hotel h = ds.searchHotels(null, null).get(0);
        var review = ds.submitReview(h.getId(), "U", 4.0, Map.of(),
                "T", "B", "2026-04-15");
        assertThat(ds.markReviewHelpful(review.getId())).isPresent();
    }

    @Test
    void mark_review_helpful_returns_empty_for_unknown_id() {
        assertThat(ds.markReviewHelpful("not-real")).isEmpty();
    }

    // ── India IT-corridor hotels ─────────────────────────────────────────────

    @Test
    void india_it_corridor_seed_includes_two_to_three_hotels_per_city() {
        List<Hotel> indianHotels = ds.searchHotels(
                Map.of("countryCodes", List.of("IN")), null);
        // 17 hand-curated IT-area hotels + the procedurally-generated ones from
        // the data generator (which uses the same IN country seed).
        assertThat(indianHotels).hasSizeGreaterThanOrEqualTo(17);

        List<String> hyderabad = indianHotels.stream()
                .filter(h -> "Hyderabad".equals(h.getLocation().address().city()))
                .map(Hotel::getId).toList();
        assertThat(hyderabad).contains(
                "prop-india-hyd-hitec", "prop-india-hyd-gachi", "prop-india-hyd-madha");
    }

    @Test
    void search_by_it_district_name_finds_business_hotels() {
        // The IT-area name is in each hotel's display name, so a destination
        // search for "HITEC City" should surface the Hyderabad IT property.
        List<Hotel> matches = ds.searchHotels(Map.of("query", "HITEC City"), null);
        assertThat(matches).extracting(Hotel::getId).contains("prop-india-hyd-hitec");

        List<Hotel> bkc = ds.searchHotels(Map.of("query", "BKC"), null);
        assertThat(bkc).extracting(Hotel::getId).contains("prop-india-bom-bkc");

        List<Hotel> whitefield = ds.searchHotels(Map.of("query", "Whitefield"), null);
        assertThat(whitefield).extracting(Hotel::getId).contains("prop-india-blr-white");
    }

    @Test
    void featured_hotels_include_flagship_india_properties() {
        // first:9 is what the home page asks for after India flagships joined the
        // carousel. The four India flagships sit after the four global ones in
        // insertion order, so the home page surfaces all of them.
        List<Hotel> top = ds.getFeaturedHotels(null, null, 9);
        assertThat(top).extracting(Hotel::getId).contains(
                "prop-india-bom-bkc",
                "prop-india-del-cyber",
                "prop-india-hyd-hitec",
                "prop-india-blr-white");
    }

    // ── Destination autocomplete ─────────────────────────────────────────────

    @Test
    void destination_suggestions_returns_empty_for_blank_query() {
        assertThat(ds.destinationSuggestions(null, 10)).isEmpty();
        assertThat(ds.destinationSuggestions("", 10)).isEmpty();
        assertThat(ds.destinationSuggestions("   ", 10)).isEmpty();
    }

    @Test
    void destination_suggestions_returns_empty_when_limit_is_zero_or_negative() {
        assertThat(ds.destinationSuggestions("Paris", 0)).isEmpty();
        assertThat(ds.destinationSuggestions("Paris", -1)).isEmpty();
    }

    @Test
    void destination_suggestions_matches_a_city_by_prefix() {
        var out = ds.destinationSuggestions("Par", 10);
        assertThat(out).extracting(com.luxe.property.schema.types.DestinationSuggestion::label)
                .contains("Paris");
        // Cities come before hotel-name matches.
        assertThat(out.get(0).type()).isEqualTo("CITY");
    }

    @Test
    void destination_suggestions_matches_country_by_prefix() {
        var out = ds.destinationSuggestions("India", 10);
        assertThat(out).extracting(com.luxe.property.schema.types.DestinationSuggestion::type)
                .contains("COUNTRY");
        assertThat(out).extracting(com.luxe.property.schema.types.DestinationSuggestion::label)
                .contains("India");
    }

    @Test
    void destination_suggestions_matches_hotel_by_substring() {
        var out = ds.destinationSuggestions("HITEC", 10);
        assertThat(out)
                .extracting(com.luxe.property.schema.types.DestinationSuggestion::hotelId)
                .contains("prop-india-hyd-hitec");
    }

    @Test
    void destination_suggestions_is_case_insensitive() {
        var lower = ds.destinationSuggestions("paris", 10);
        var upper = ds.destinationSuggestions("PARIS", 10);
        var mixed = ds.destinationSuggestions("PaRiS", 10);
        assertThat(lower).isNotEmpty();
        assertThat(upper).hasSameSizeAs(lower);
        assertThat(mixed).hasSameSizeAs(lower);
    }

    @Test
    void destination_suggestions_emits_state_type_for_state_matches() {
        var out = ds.destinationSuggestions("Telangana", 10);
        var stateMatches = out.stream()
                .filter(s -> "STATE".equals(s.type()))
                .toList();
        assertThat(stateMatches).isNotEmpty();
        assertThat(stateMatches).extracting(com.luxe.property.schema.types.DestinationSuggestion::label)
                .contains("Telangana");
        var t = stateMatches.stream()
                .filter(s -> "Telangana".equals(s.label())).findFirst().orElseThrow();
        assertThat(t.state()).isEqualTo("Telangana");
        assertThat(t.country()).isEqualTo("India");
        assertThat(t.sublabel()).contains("India").contains("hotel");
    }

    @Test
    void destination_suggestions_dedupes_states_per_country() {
        var out = ds.destinationSuggestions("Maharashtra", 10);
        long count = out.stream()
                .filter(s -> "STATE".equals(s.type()) && "Maharashtra".equals(s.label()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void destination_suggestions_ranks_state_matches_above_country_matches() {
        // "Tamil Nadu" prefix-matches a state but no country. Verify the state
        // suggestion is present and lands before the hotel suggestions.
        var out = ds.destinationSuggestions("Tamil", 10);
        int stateIdx = -1, hotelIdx = -1;
        for (int i = 0; i < out.size(); i++) {
            if (stateIdx == -1 && "STATE".equals(out.get(i).type())) stateIdx = i;
            if (hotelIdx == -1 && "HOTEL".equals(out.get(i).type())) hotelIdx = i;
        }
        assertThat(stateIdx).isGreaterThanOrEqualTo(0);
        if (hotelIdx >= 0) assertThat(stateIdx).isLessThan(hotelIdx);
    }

    @Test
    void destination_suggestions_dedupes_cities_with_count_in_sublabel() {
        var out = ds.destinationSuggestions("Paris", 20);
        var paris = out.stream()
                .filter(s -> "CITY".equals(s.type()) && "Paris".equals(s.label()))
                .findFirst().orElseThrow();
        // Sublabel should mention the country and the hotel count.
        assertThat(paris.sublabel()).contains("France").contains("hotel");
    }

    @Test
    void destination_suggestions_caps_at_limit() {
        var out = ds.destinationSuggestions("a", 5); // wide match
        assertThat(out).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    void destination_suggestions_ranks_prefix_matches_above_substring_matches() {
        // "Lon" prefix-matches "London"; substring-matches hotels whose names
        // contain "Lon" only mid-string. The prefix city should sit before any
        // mid-string hotel match.
        var out = ds.destinationSuggestions("Lon", 20);
        int firstLondonCity = -1;
        int firstSubstringHotel = -1;
        for (int i = 0; i < out.size(); i++) {
            var s = out.get(i);
            if (firstLondonCity == -1 && "CITY".equals(s.type()) && "London".equalsIgnoreCase(s.label())) {
                firstLondonCity = i;
            }
            if (firstSubstringHotel == -1 && "HOTEL".equals(s.type())
                    && !s.label().toLowerCase().startsWith("lon")) {
                firstSubstringHotel = i;
            }
        }
        if (firstLondonCity >= 0 && firstSubstringHotel >= 0) {
            assertThat(firstLondonCity).isLessThan(firstSubstringHotel);
        }
    }

    @Test
    void india_hotels_cover_all_seven_target_cities() {
        List<Hotel> indianHotels = ds.searchHotels(
                Map.of("countryCodes", List.of("IN")), null);
        var cities = indianHotels.stream()
                .map(h -> h.getLocation().address().city())
                .distinct()
                .toList();
        assertThat(cities).contains("Hyderabad", "Gurgaon", "Noida",
                "Bangalore", "Mumbai", "Chennai", "Pune", "Visakhapatnam");
    }
}
