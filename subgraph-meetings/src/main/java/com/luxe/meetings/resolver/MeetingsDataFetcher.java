package com.luxe.meetings.resolver;

import com.luxe.meetings.schema.types.Hotel;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthContextResolver;
import com.luxe.common.error.NotFoundError;
import com.luxe.common.error.ValidationError;
import com.luxe.common.error.FieldError;
import com.luxe.common.pagination.Connection;
import com.luxe.meetings.datasource.MeetingsDataSource;
import com.luxe.meetings.schema.types.*;
import com.netflix.graphql.dgs.*;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DgsComponent
public class MeetingsDataFetcher {

    private final MeetingsDataSource dataSource;
    private final AuthContextResolver authResolver;

    public MeetingsDataFetcher(MeetingsDataSource dataSource, AuthContextResolver authResolver) {
        this.dataSource = dataSource;
        this.authResolver = authResolver;
    }

    private AuthContext getAuth(DataFetchingEnvironment dfe) {
        return authResolver.resolve(dfe);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @DgsQuery
    public List<EventSpace> eventSpaces(@InputArgument String hotelId,
                                          @InputArgument Map<String, Object> filter) {
        return dataSource.findSpacesByHotel(hotelId, filter);
    }

    @DgsQuery
    public EventSpace eventSpace(@InputArgument String id) {
        return dataSource.findSpaceById(id).orElse(null);
    }

    @DgsQuery
    public Map<String, Object> searchEventSpaces(@InputArgument Map<String, Object> input) {
        List<EventSpace> hits = dataSource.searchSpaces(input);
        List<Map<String, Object>> results = hits.stream()
                .<Map<String, Object>>map(s -> Map.of(
                        "hotel", Map.of("id", s.hotelId()),
                        "space", s,
                        "matchScore", scoreMatch(s, input),
                        "notes", "Match based on requested setup and attendee count"))
                .toList();
        return Map.of(
                "results", results,
                "totalCount", hits.size());
    }

    @SuppressWarnings("unchecked")
    private double scoreMatch(EventSpace s, Map<String, Object> input) {
        int attendees = input.get("attendees") != null
                ? ((Number) input.get("attendees")).intValue() : 0;
        String setup = (String) input.get("setup");
        return s.capacityStyles().stream()
                .filter(c -> setup == null || c.setup().equals(setup))
                .mapToDouble(c -> Math.max(0.0, 1.0 - Math.abs(c.capacity() - attendees) / 1000.0))
                .max().orElse(0.0);
    }

    @DgsQuery
    public List<CateringMenu> cateringMenus(@InputArgument String hotelId) {
        return dataSource.findCateringMenus(hotelId);
    }

    @DgsQuery
    public RFP rfp(@InputArgument String id) {
        return dataSource.findRFPById(id).orElse(null);
    }

    @DgsQuery
    public Object myRFPs(@InputArgument String status,
                           @InputArgument Integer first, @InputArgument String after,
                           DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        String email = auth.guestId() != null ? auth.guestId() + "@example.com" : "unknown@example.com";
        // Mock: lookup uses contact email; tests can use known emails directly
        List<RFP> all = dataSource.findRFPsByOrganizer(email, status);
        Connection<RFP> conn = Connection.of(all, first != null ? first : 10, after);
        return Map.of(
                "edges", conn.edges().stream()
                        .map(e -> Map.of("node", e.node(), "cursor", e.cursor())).toList(),
                "pageInfo", pageInfo(conn),
                "totalCount", conn.totalCount());
    }

    @DgsQuery
    public GroupBlock groupBlock(@InputArgument String id) {
        return dataSource.findGroupBlockById(id).orElse(null);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @DgsMutation
    public Object submitRFP(@InputArgument Map<String, Object> input,
                              @InputArgument String idempotencyKey,
                              DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        if (input.get("organizer") == null || input.get("eventName") == null
                || input.get("startDate") == null || input.get("endDate") == null) {
            return new ValidationError("INVALID_INPUT",
                    "organizer, eventName, startDate, endDate are required",
                    List.of(new FieldError("organizer", "Required")));
        }
        String email = (String) input.getOrDefault("contactEmail", auth.guestId());
        return dataSource.submitRFP(input, email);
    }

    @DgsMutation
    public Object updateRFP(@InputArgument String rfpId,
                              @InputArgument Map<String, Object> input,
                              DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        if (dataSource.findRFPById(rfpId).isEmpty())
            return new NotFoundError("RFP", rfpId);
        return dataSource.updateRFP(rfpId, input);
    }

    @DgsMutation
    public Object cancelRFP(@InputArgument String rfpId, @InputArgument String reason,
                              DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        if (dataSource.findRFPById(rfpId).isEmpty())
            return new NotFoundError("RFP", rfpId);
        return dataSource.cancelRFP(rfpId, reason, auth.guestId());
    }

    @DgsMutation
    public Object bookGroupRoom(@InputArgument Map<String, Object> input,
                                  @InputArgument String idempotencyKey,
                                  DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        if (input.get("hotelId") == null || input.get("rooms") == null) {
            return new ValidationError("INVALID_INPUT",
                    "hotelId and rooms are required", List.of());
        }
        return dataSource.createGroupBlock(input);
    }

    // ── Federation ────────────────────────────────────────────────────────────

    @DgsData(parentType = "Hotel", field = "eventSpaces")
    public List<EventSpace> hotelEventSpaces(DataFetchingEnvironment dfe) {
        Hotel hotel = dfe.getSource();
        Map<String, Object> filter = dfe.getArgument("filter");
        return dataSource.findSpacesByHotel(hotel.getId(), filter);
    }

    @DgsEntityFetcher(name = "EventSpace")
    public EventSpace fetchEventSpace(Map<String, Object> values) {
        return dataSource.findSpaceById((String) values.get("id")).orElse(null);
    }

    @DgsEntityFetcher(name = "RFP")
    public RFP fetchRFP(Map<String, Object> values) {
        return dataSource.findRFPById((String) values.get("id")).orElse(null);
    }

    @DgsEntityFetcher(name = "GroupBlock")
    public GroupBlock fetchGroupBlock(Map<String, Object> values) {
        return dataSource.findGroupBlockById((String) values.get("id")).orElse(null);
    }

    private Map<String, Object> pageInfo(Connection<?> conn) {
        HashMap<String, Object> m = new HashMap<>();
        m.put("hasNextPage", conn.pageInfo().hasNextPage());
        m.put("hasPreviousPage", conn.pageInfo().hasPreviousPage());
        m.put("startCursor", conn.pageInfo().startCursor());
        m.put("endCursor", conn.pageInfo().endCursor());
        return m;
    }
    @DgsEntityFetcher(name = "Hotel")
    public Hotel fetchHotelReference(Map<String, Object> values) {
        return new Hotel((String) values.get("id"));
    }

}
