package com.luxe.property.resolver;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthContextResolver;
import com.luxe.common.auth.AuthRole;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolver-level test: spins up the full Spring context for the property subgraph
 * and executes real GraphQL operations through DGS. Auth is overridden via a
 * {@link TestConfiguration} so the auth-gated mutations (submitReview) execute
 * their bodies, not just the auth gate.
 */
@SpringBootTest
@Import(PropertyDataFetcherTest.AuthOverrideConfig.class)
class PropertyDataFetcherTest {

    @TestConfiguration
    static class AuthOverrideConfig {
        @Bean @Primary
        AuthContextResolver mockAuth() {
            return dfe -> AuthContext.of("guest-001", "LUX0001234567", AuthRole.GUEST);
        }
    }

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
    void hotels_filter_by_country_code_returns_french_hotels() {
        // Regression: HotelFilter.countryCodes is [CountryCode!], which DGS
        // hands to the resolver as List<CountryCode> rather than List<String>.
        // Plain List<String>.contains(...) on the address's String countryCode
        // would always return false and yield zero hits — which is what /hotels
        // showed when picking a country with no city.
        Integer total = dgs.executeAndExtractJsonPath(
                "{ hotels(filter: { countryCodes: [\"FR\"] }, first: 50) { totalCount } }",
                "data.hotels.totalCount");
        assertThat(total).isNotNull().isGreaterThan(0);
        // Every hit must actually live in FR.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = dgs.executeAndExtractJsonPath(
                """
                { hotels(filter: { countryCodes: ["FR"] }, first: 50) {
                    edges { node { id location { address { countryCode } } } }
                } }
                """,
                "data.hotels.edges[*].node");
        assertThat(nodes).isNotEmpty();
        for (Map<String, Object> n : nodes) {
            @SuppressWarnings("unchecked")
            Map<String, Object> address = (Map<String, Object>)
                    ((Map<String, Object>) n.get("location")).get("address");
            assertThat(address.get("countryCode")).isEqualTo("FR");
        }
    }

    @Test
    void hotels_filter_by_multiple_country_codes_returns_union() {
        Integer total = dgs.executeAndExtractJsonPath(
                "{ hotels(filter: { countryCodes: [\"FR\", \"JP\"] }, first: 50) { totalCount } }",
                "data.hotels.totalCount");
        Integer fr = dgs.executeAndExtractJsonPath(
                "{ hotels(filter: { countryCodes: [\"FR\"] }, first: 50) { totalCount } }",
                "data.hotels.totalCount");
        Integer jp = dgs.executeAndExtractJsonPath(
                "{ hotels(filter: { countryCodes: [\"JP\"] }, first: 50) { totalCount } }",
                "data.hotels.totalCount");
        assertThat(total).isEqualTo(fr + jp);
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
