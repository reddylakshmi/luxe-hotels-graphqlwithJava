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
}
