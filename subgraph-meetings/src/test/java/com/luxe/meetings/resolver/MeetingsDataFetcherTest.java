package com.luxe.meetings.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MeetingsDataFetcherTest {

    @Autowired
    DgsQueryExecutor dgs;

    @Test
    void event_spaces_for_hotel_returns_data() {
        List<Map<String, Object>> spaces = dgs.executeAndExtractJsonPath(
                """
                { eventSpaces(hotelId: "prop-paris-001") {
                    id name capacityStyles { setup capacity } rateCard { fullDay { amount currency } }
                } }
                """,
                "data.eventSpaces");
        assertThat(spaces).isNotEmpty();
    }

    @Test
    void rfp_query_returns_seeded_rfp() {
        String status = dgs.executeAndExtractJsonPath(
                "{ rfp(id: \"rfp-001\") { id status eventName } }",
                "data.rfp.status");
        assertThat(status).isNotBlank();
    }

    @Test
    void search_event_spaces_returns_hits() {
        Integer total = dgs.executeAndExtractJsonPath(
                """
                { searchEventSpaces(input: {
                    startDate: "2026-08-01", endDate: "2026-08-03",
                    attendees: 200, setup: THEATER
                }) { totalCount } }
                """,
                "data.searchEventSpaces.totalCount");
        assertThat(total).isNotNull();
    }

    @Test
    void federation_entities_resolves_hotel_extension() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on Hotel { eventSpaces { name } }
                  }
                }
                """;
        var rep = Map.of("__typename", "Hotel", "id", "prop-paris-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
    }
}
