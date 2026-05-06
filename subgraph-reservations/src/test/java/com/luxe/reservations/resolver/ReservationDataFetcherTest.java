package com.luxe.reservations.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReservationDataFetcherTest {

    @Autowired
    DgsQueryExecutor dgs;

    @Test
    void reservation_query_requires_authentication() {
        // Anonymous → resolver throws UnauthorizedException; we expect an error in result
        var result = dgs.execute("{ reservation(id: \"res-001\") { id } }");
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getMessage())
                .containsIgnoringCase("Authentication");
    }

    @Test
    void reservation_by_confirmation_number_does_not_require_auth() {
        String result = dgs.executeAndExtractJsonPath(
                """
                { reservationByConfirmationNumber(confirmationNumber: "LUX-2025-100002") {
                    id confirmationNumber status
                } }
                """,
                "data.reservationByConfirmationNumber.id");
        assertThat(result).isEqualTo("res-002");
    }

    @Test
    void federation_entities_resolves_reservation_by_key() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on Reservation { id status }
                  }
                }
                """;
        var rep = Map.of("__typename", "Reservation", "id", "res-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities =
                (List<Map<String, Object>>) ((Map<String, Object>) result.getData()).get("_entities");
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).get("id")).isEqualTo("res-001");
    }
}
