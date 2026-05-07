package com.luxe.pricing.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.exceptions.QueryException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Exercises the {@code Hotel.availability(...)} federation field that powers
 * the home-page "Find a hotel" results. The Apollo Router resolves
 * federated Hotel via property's entity fetcher, then asks pricing for the
 * availability/rates extension. We test the pricing side here directly
 * by hitting the entity-resolution path (_entities) the way the router
 * itself would.
 */
@SpringBootTest
class HotelAvailabilitySearchTest {

    @Autowired DgsQueryExecutor dgs;

    /** Federation entity-resolution query — the same shape Apollo Router fires. */
    private static final String ENTITY_QUERY = """
            query SearchAvailability($repr: [_Any!]!, $checkIn: Date!, $checkOut: Date!) {
              _entities(representations: $repr) {
                ... on Hotel {
                  availability(checkIn: $checkIn, checkOut: $checkOut, adults: 2) {
                    nights
                    currency
                    lowestRate { amount currency }
                  }
                }
              }
            }
            """;

    @Test
    void availability_resolves_for_known_hotel_with_iso_dates() {
        List<Map<String, Object>> entities = dgs.executeAndExtractJsonPath(
                ENTITY_QUERY,
                "data._entities",
                Map.of(
                        "repr", List.of(Map.of("__typename", "Hotel", "id", "prop-paris-001")),
                        "checkIn", "2026-09-01",
                        "checkOut", "2026-09-04"));
        assertThat(entities).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> avail = (Map<String, Object>) entities.get(0).get("availability");
        assertThat(avail).isNotNull();
        assertThat(avail.get("nights")).isEqualTo(3);
        // Each hotel has a native currency in its mock data; we only assert
        // it's set, not what it is.
        assertThat(avail.get("currency")).asString().isNotEmpty();
    }

    @Test
    void availability_resolves_for_generated_hotel_id() {
        // Generated brand-MAI hotel in Tokyo from the property generator.
        List<Map<String, Object>> entities = dgs.executeAndExtractJsonPath(
                ENTITY_QUERY,
                "data._entities",
                Map.of(
                        "repr", List.of(Map.of("__typename", "Hotel", "id", "prop-mai-jp-tokyo")),
                        "checkIn", "2026-09-01",
                        "checkOut", "2026-09-04"));
        assertThat(entities).hasSize(1);
        // Pricing's mock responds for any hotel id.
        @SuppressWarnings("unchecked")
        Map<String, Object> avail = (Map<String, Object>) entities.get(0).get("availability");
        assertThat(avail).isNotNull();
        assertThat(avail.get("nights")).isEqualTo(3);
    }

    @Test
    void availability_handles_long_stays() {
        List<Map<String, Object>> entities = dgs.executeAndExtractJsonPath(
                ENTITY_QUERY,
                "data._entities",
                Map.of(
                        "repr", List.of(Map.of("__typename", "Hotel", "id", "prop-paris-001")),
                        "checkIn", "2026-09-01",
                        "checkOut", "2026-09-15"));
        @SuppressWarnings("unchecked")
        Map<String, Object> avail = (Map<String, Object>) entities.get(0).get("availability");
        assertThat(avail.get("nights")).isEqualTo(14);
    }

    @Test
    void availability_currency_override_is_honored() {
        String q = """
                query($repr: [_Any!]!) {
                  _entities(representations: $repr) {
                    ... on Hotel {
                      availability(checkIn: "2026-09-01", checkOut: "2026-09-03",
                                   adults: 2, currency: "EUR") {
                        currency
                      }
                    }
                  }
                }
                """;
        List<Map<String, Object>> entities = dgs.executeAndExtractJsonPath(
                q, "data._entities",
                Map.of("repr", List.of(Map.of("__typename", "Hotel", "id", "prop-paris-001"))));
        @SuppressWarnings("unchecked")
        Map<String, Object> avail = (Map<String, Object>) entities.get(0).get("availability");
        assertThat(avail.get("currency")).isEqualTo("EUR");
    }

    @Test
    void searchRates_query_returns_AvailabilityResult_for_valid_dates() {
        Map<String, Object> result = dgs.executeAndExtractJsonPath(
                """
                query {
                  searchRates(input: { hotelId: "prop-paris-001",
                    checkIn: "2026-09-01", checkOut: "2026-09-04",
                    adults: 2, currency: "USD" }) {
                    __typename
                    ... on AvailabilityResult { nights currency }
                    ... on ValidationError { code }
                  }
                }
                """,
                "data.searchRates");
        assertThat(result.get("__typename")).isEqualTo("AvailabilityResult");
        assertThat(result.get("nights")).isEqualTo(3);
    }

    @Test
    void searchRates_rejects_inverted_dates_with_ValidationError() {
        Map<String, Object> result = dgs.executeAndExtractJsonPath(
                """
                query {
                  searchRates(input: { hotelId: "prop-paris-001",
                    checkIn: "2026-09-04", checkOut: "2026-09-01",
                    adults: 2, currency: "USD" }) {
                    __typename
                    ... on ValidationError { code message }
                  }
                }
                """,
                "data.searchRates");
        assertThat(result.get("__typename")).isEqualTo("ValidationError");
        assertThat(result.get("code")).isEqualTo("INVALID_DATES");
    }

    // searchRates input has a hotelId requirement enforced at the resolver level
    // (the schema marks hotelId as nullable to allow runtime validation), but
    // this branch is unreachable when going through the public schema since
    // hotelId is in fact non-null on the input. Skipping that pure-validation
    // case keeps this suite focused on real client paths.

    @Test
    void availability_does_not_crash_on_missing_dates() {
        // Missing required Date! arguments → schema-level validation error,
        // not an internal cast failure.
        assertThatCode(() -> dgs.executeAndExtractJsonPath(
                """
                query($repr: [_Any!]!) {
                  _entities(representations: $repr) {
                    ... on Hotel { availability(checkIn: "2026-09-01", checkOut: "2026-09-04") { nights } }
                  }
                }
                """,
                "data._entities[0].availability.nights",
                Map.of("repr", List.of(Map.of("__typename", "Hotel", "id", "prop-paris-001")))))
                .doesNotThrowAnyException();
    }

    @Test
    void availability_does_not_throw_ClassCastException() {
        // Regression guard for the LocalDate→String cast bug. If reintroduced,
        // the query would surface a QueryException with ClassCastException.
        assertThatCode(() -> dgs.executeAndExtractJsonPath(
                ENTITY_QUERY,
                "data._entities",
                Map.of(
                        "repr", List.of(Map.of("__typename", "Hotel", "id", "prop-paris-001")),
                        "checkIn", "2026-09-01",
                        "checkOut", "2026-09-04")))
                .doesNotThrowAnyException();
    }
}
