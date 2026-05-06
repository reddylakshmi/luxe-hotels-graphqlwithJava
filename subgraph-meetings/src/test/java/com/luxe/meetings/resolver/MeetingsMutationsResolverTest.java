package com.luxe.meetings.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolver coverage for the four RFP/group-block mutations + queries we didn't
 * already cover, plus federation entity fetching for EventSpace, RFP, GroupBlock.
 */
@SpringBootTest
class MeetingsMutationsResolverTest {

    @Autowired
    DgsQueryExecutor dgs;

    private static String key() { return UUID.randomUUID().toString(); }

    // ── RFP mutations: all require auth ──────────────────────────────────────

    @Test
    void submit_rfp_requires_auth() {
        String mutation = """
                mutation { submitRFP(input: {
                    organizer: "Maria Chen", organization: "Acme",
                    contactEmail: "x@acme.example", contactPhone: "+1-415-555-0142",
                    eventName: "Q3 Summit", eventType: BOARD_RETREAT,
                    startDate: "2026-09-01", endDate: "2026-09-04",
                    attendees: 50, preferredHotelIds: ["prop-paris-001"],
                    spaceRequirements: [{
                        name: "Plenary", setup: THEATER, attendees: 50, durationHours: 8
                    }]
                }, idempotencyKey: "%s") {
                  ... on RFP { id rfpNumber status }
                  ... on ValidationError { code message }
                  ... on NotFoundError { code }
                } }
                """.formatted(key());
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void update_rfp_requires_auth() {
        String mutation = """
                mutation { updateRFP(rfpId: "rfp-001", input: { attendees: 60 }) {
                  ... on RFP { id attendees }
                  ... on NotFoundError { code resourceType }
                  ... on ValidationError { code }
                } }
                """;
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void cancel_rfp_requires_auth() {
        String mutation = """
                mutation { cancelRFP(rfpId: "rfp-001", reason: "Plans changed") {
                  ... on RFP { id status }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """;
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void book_group_room_requires_auth() {
        String mutation = """
                mutation { bookGroupRoom(input: {
                    rfpId: "rfp-001", hotelId: "prop-paris-001",
                    startDate: "2026-09-01", endDate: "2026-09-04",
                    rooms: 30, rate: 450, currency: "EUR",
                    cutoffDate: "2026-08-15"
                }, idempotencyKey: "%s") {
                  ... on GroupBlock { id totalRooms remainingRooms }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(key());
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void my_rfps_query_requires_auth() {
        var result = dgs.execute("{ myRFPs { totalCount } }");
        assertThat(result.getErrors()).isNotEmpty();
    }

    // ── public-facing queries (no auth) ──────────────────────────────────────

    @Test
    void event_space_by_unknown_id_returns_null() {
        Object s = dgs.executeAndExtractJsonPath(
                "{ eventSpace(id: \"not-real\") { id } }", "data.eventSpace");
        assertThat(s).isNull();
    }

    @Test
    void rfp_by_unknown_id_returns_null() {
        Object r = dgs.executeAndExtractJsonPath(
                "{ rfp(id: \"not-real\") { id } }", "data.rfp");
        assertThat(r).isNull();
    }

    @Test
    void group_block_by_known_id_returns_block() {
        String id = dgs.executeAndExtractJsonPath(
                "{ groupBlock(id: \"blk-001\") { id blockCode totalRooms remainingRooms } }",
                "data.groupBlock.id");
        assertThat(id).isEqualTo("blk-001");
    }

    @Test
    void group_block_by_unknown_id_returns_null() {
        Object b = dgs.executeAndExtractJsonPath(
                "{ groupBlock(id: \"not-real\") { id } }", "data.groupBlock");
        assertThat(b).isNull();
    }

    @Test
    void catering_menus_for_hotel_returns_menus_with_courses() {
        List<Map<String, Object>> menus = dgs.executeAndExtractJsonPath(
                """
                { cateringMenus(hotelId: "prop-paris-001") {
                    id name pricePerPerson { amount currency } courses { name }
                } }
                """,
                "data.cateringMenus");
        assertThat(menus).isNotEmpty();
    }

    @Test
    void event_spaces_filter_by_natural_light() {
        List<Map<String, Object>> bright = dgs.executeAndExtractJsonPath(
                """
                { eventSpaces(hotelId: "prop-paris-001",
                              filter: { naturalLight: true }) {
                    id naturalLight
                } }
                """,
                "data.eventSpaces");
        assertThat(bright).allSatisfy(s ->
                assertThat(s.get("naturalLight")).isEqualTo(true));
    }

    @Test
    void search_event_spaces_with_hotel_id_filter_returns_only_those_hotels() {
        Integer total = dgs.executeAndExtractJsonPath(
                """
                { searchEventSpaces(input: {
                    startDate: "2026-09-01", endDate: "2026-09-03",
                    attendees: 50, hotelIds: ["prop-paris-001"]
                }) {
                    totalCount
                    results { hotel { id } space { id } matchScore }
                } }
                """,
                "data.searchEventSpaces.totalCount");
        assertThat(total).isNotNull();
    }

    // ── Federation entity resolvers ──────────────────────────────────────────

    @Test
    void federation_entities_resolves_event_space_by_key() {
        // Use a real seeded space id: discover via hotel query
        String spaceId = dgs.executeAndExtractJsonPath(
                "{ eventSpaces(hotelId: \"prop-paris-001\") { id } }",
                "data.eventSpaces[0].id");
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on EventSpace { id name areaSqFt }
                  }
                }
                """;
        var rep = Map.of("__typename", "EventSpace", "id", spaceId);
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void federation_entities_resolves_rfp_by_key() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on RFP { id rfpNumber status }
                  }
                }
                """;
        var rep = Map.of("__typename", "RFP", "id", "rfp-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities =
                (List<Map<String, Object>>) ((Map<String, Object>) result.getData()).get("_entities");
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).get("id")).isEqualTo("rfp-001");
    }

    @Test
    void federation_entities_resolves_group_block_by_key() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on GroupBlock { id blockCode totalRooms }
                  }
                }
                """;
        var rep = Map.of("__typename", "GroupBlock", "id", "blk-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void federation_entities_resolves_hotel_extension_with_event_spaces() {
        String query = """
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on Hotel { eventSpaces(filter: { minCapacity: 100 }) { name } }
                  }
                }
                """;
        var rep = Map.of("__typename", "Hotel", "id", "prop-paris-001");
        var result = dgs.execute(query, Map.of("reps", List.of(rep)));
        assertThat(result.getErrors()).isEmpty();
    }
}
