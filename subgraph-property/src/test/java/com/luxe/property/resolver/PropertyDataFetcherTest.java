package com.luxe.property.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolver-level test: spins up the full Spring context for the property subgraph
 * and executes real GraphQL operations through DGS. This exercises schema parsing,
 * custom scalar coercion, resolver dispatch, and federation entity resolution all in one.
 */
@SpringBootTest
class PropertyDataFetcherTest {

    @Autowired
    DgsQueryExecutor dgs;

    // ── Plain queries ────────────────────────────────────────────────────────

    @Test
    void hotel_by_id_returns_seeded_paris_hotel() {
        String query = """
                { hotel(id: "prop-paris-001") {
                    id name starRating
                    location { address { city countryCode } coordinates { latitude longitude } }
                  } }
                """;
        String name = dgs.executeAndExtractJsonPath(query, "data.hotel.name");
        String city = dgs.executeAndExtractJsonPath(query, "data.hotel.location.address.city");
        Integer stars = dgs.executeAndExtractJsonPath(query, "data.hotel.starRating");
        assertThat(name).contains("Paris");
        assertThat(city).isEqualTo("Paris");
        assertThat(stars).isEqualTo(5);
    }

    @Test
    void hotel_by_id_returns_null_for_unknown() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ hotel(id: \"does-not-exist\") { id } }", "data.hotel");
        assertThat(result).isNull();
    }

    @Test
    void featured_hotels_respects_first_argument() {
        List<Map<String, Object>> hotels = dgs.executeAndExtractJsonPath(
                "{ featuredHotels(first: 2) { id name } }", "data.featuredHotels");
        assertThat(hotels).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    void featured_hotels_filters_by_country_code() {
        List<Map<String, Object>> hotels = dgs.executeAndExtractJsonPath(
                "{ featuredHotels(countryCode: \"FR\") { id location { address { countryCode } } } }",
                "data.featuredHotels");
        assertThat(hotels).isNotEmpty();
        assertThat(hotels).allSatisfy(h -> {
            Map<String, Object> location = (Map<String, Object>) h.get("location");
            Map<String, Object> address = (Map<String, Object>) location.get("address");
            assertThat(address.get("countryCode")).isEqualTo("FR");
        });
    }

    @Test
    void hotels_query_returns_a_relay_connection() {
        String query = "{ hotels(first: 2) { edges { node { id name } cursor } pageInfo { hasNextPage } totalCount } }";
        Integer total = dgs.executeAndExtractJsonPath(query, "data.hotels.totalCount");
        List<Object> edges = dgs.executeAndExtractJsonPath(query, "data.hotels.edges");
        Boolean hasNext = dgs.executeAndExtractJsonPath(query, "data.hotels.pageInfo.hasNextPage");
        assertThat(total).isPositive();
        assertThat(edges).hasSize(2);
        assertThat(hasNext).isTrue();
    }

    @Test
    void hotels_search_filter_by_min_star_rating() {
        String query = """
                { hotels(filter: { minStarRating: 5 }) { edges { node { starRating } } } }
                """;
        List<Map<String, Object>> edges = dgs.executeAndExtractJsonPath(query, "data.hotels.edges");
        assertThat(edges).isNotEmpty();
        assertThat(edges).allSatisfy(e -> {
            Map<String, Object> node = (Map<String, Object>) e.get("node");
            assertThat((Integer) node.get("starRating")).isGreaterThanOrEqualTo(5);
        });
    }

    @Test
    void hotel_by_slug_resolves() {
        // get a known slug then fetch by it
        String anySlug = dgs.executeAndExtractJsonPath(
                "{ featuredHotels(first: 1) { slug } }", "data.featuredHotels[0].slug");
        assertThat(anySlug).isNotBlank();
        String name = dgs.executeAndExtractJsonPath(
                "{ hotelBySlug(slug: \"" + anySlug + "\") { name } }", "data.hotelBySlug.name");
        assertThat(name).isNotBlank();
    }

    @Test
    void brand_by_code_returns_brand() {
        String anyCode = dgs.executeAndExtractJsonPath(
                "{ brands { edges { node { code } } } }", "data.brands.edges[0].node.code");
        assertThat(anyCode).isNotBlank();
        String name = dgs.executeAndExtractJsonPath(
                "{ brandByCode(code: \"" + anyCode + "\") { code name } }",
                "data.brandByCode.name");
        assertThat(name).isNotBlank();
    }

    // ── Federation entity resolution ─────────────────────────────────────────

    @Test
    void federation_entities_query_resolves_hotel_by_key() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on Hotel { id name starRating }
                  }
                }
                """;
        var rep = Map.of("__typename", "Hotel", "id", "prop-paris-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities =
                (List<Map<String, Object>>) ((Map<String, Object>) result.getData()).get("_entities");
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).get("id")).isEqualTo("prop-paris-001");
        assertThat(entities.get(0).get("starRating")).isEqualTo(5);
    }

    @Test
    void federation_entities_query_resolves_brand_by_key() {
        String brandId = dgs.executeAndExtractJsonPath(
                "{ brands { edges { node { id } } } }", "data.brands.edges[0].node.id");
        assertThat(brandId).isNotBlank();
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on Brand { id code name }
                  }
                }
                """;
        var rep = Map.of("__typename", "Brand", "id", brandId);
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    @Test
    void submit_review_against_unknown_hotel_returns_not_found() {
        String mutation = """
                mutation { submitReview(input: {
                  hotelId: "no-such-hotel", overallRating: 4.5,
                  title: "t", body: "b", stayDate: "2026-04-15"
                }) {
                  ... on NotFoundError { __typename code resourceType }
                  ... on Review { id }
                } }
                """;
        String typename = dgs.executeAndExtractJsonPath(mutation, "data.submitReview.__typename");
        assertThat(typename).isEqualTo("NotFoundError");
    }

    @Test
    void submit_review_with_valid_input_persists_and_returns_review() {
        String mutation = """
                mutation { submitReview(input: {
                  hotelId: "prop-paris-001", overallRating: 4.7,
                  title: "Lovely", body: "A truly memorable stay.",
                  stayDate: "2026-03-20"
                }) {
                  __typename
                  ... on Review { id title overallRating }
                } }
                """;
        String typename = dgs.executeAndExtractJsonPath(mutation, "data.submitReview.__typename");
        Double rating = dgs.executeAndExtractJsonPath(mutation, "data.submitReview.overallRating");
        assertThat(typename).isEqualTo("Review");
        assertThat(rating).isEqualTo(4.7);
    }
}
