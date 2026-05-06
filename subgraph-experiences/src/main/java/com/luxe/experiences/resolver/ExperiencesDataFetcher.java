package com.luxe.experiences.resolver;

import com.luxe.experiences.schema.types.Hotel;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthContextResolver;
import com.luxe.common.error.NotFoundError;
import com.luxe.common.error.SlotUnavailableError;
import com.luxe.common.error.ValidationError;
import com.luxe.common.error.FieldError;
import com.luxe.common.pagination.Connection;
import com.luxe.experiences.datasource.ExperiencesDataSource;
import com.luxe.experiences.schema.types.*;
import com.netflix.graphql.dgs.*;
import graphql.schema.DataFetchingEnvironment;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DgsComponent
public class ExperiencesDataFetcher {

    private final ExperiencesDataSource dataSource;
    private final AuthContextResolver authResolver;

    public ExperiencesDataFetcher(ExperiencesDataSource dataSource, AuthContextResolver authResolver) {
        this.dataSource = dataSource;
        this.authResolver = authResolver;
    }

    private AuthContext getAuth(DataFetchingEnvironment dfe) {
        return authResolver.resolve(dfe);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @DgsQuery
    public List<Experience> experiences(@InputArgument String hotelId,
                                          @InputArgument String category,
                                          @InputArgument LocalDate date) {
        return dataSource.findExperiences(hotelId, category, date);
    }

    @DgsQuery
    public Experience experience(@InputArgument String id) {
        return dataSource.findExperienceById(id).orElse(null);
    }

    @DgsQuery
    public ExperienceAvailability experienceAvailability(@InputArgument String experienceId,
                                                           @InputArgument LocalDate date,
                                                           @InputArgument int partySize) {
        return dataSource.availability(experienceId, date, partySize);
    }

    @DgsQuery
    public List<SpaTreatment> spaTreatmentMenu(@InputArgument String hotelId) {
        return dataSource.spaTreatments(hotelId);
    }

    @DgsQuery
    public RestaurantAvailability restaurantAvailability(@InputArgument String hotelId,
                                                           @InputArgument LocalDate date,
                                                           @InputArgument int partySize) {
        return dataSource.restaurantAvailability(hotelId, date, partySize);
    }

    @DgsQuery
    public GolfTeeTimeAvailability golfTeeTimeAvailability(@InputArgument String hotelId,
                                                             @InputArgument LocalDate date,
                                                             @InputArgument int players) {
        return dataSource.golfAvailability(hotelId, date, players);
    }

    @DgsQuery
    public Object myExperienceBookings(@InputArgument Integer first,
                                         @InputArgument String after,
                                         @InputArgument Boolean upcoming,
                                         DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        List<ExperienceBooking> all = dataSource.findBookingsByGuestId(auth.guestId(), upcoming);
        Connection<ExperienceBooking> conn = Connection.of(all, first != null ? first : 10, after);
        return Map.of(
                "edges", conn.edges().stream()
                        .map(e -> Map.of("node", e.node(), "cursor", e.cursor())).toList(),
                "pageInfo", pageInfo(conn),
                "totalCount", conn.totalCount());
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @DgsMutation
    public Object bookExperience(@InputArgument Map<String, Object> input,
                                   @InputArgument String idempotencyKey,
                                   DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        String experienceId = (String) input.get("experienceId");
        String slotToken = (String) input.get("slotToken");
        Integer participants = input.get("participants") != null
                ? ((Number) input.get("participants")).intValue() : null;
        if (experienceId == null || slotToken == null || participants == null || participants <= 0) {
            return new ValidationError("INVALID_INPUT",
                    "experienceId, slotToken, and positive participants are required",
                    List.of(new FieldError("participants", "Must be > 0")));
        }
        if (dataSource.findExperienceById(experienceId).isEmpty())
            return new NotFoundError("Experience", experienceId);
        if (!dataSource.isSlotValid(slotToken))
            return new SlotUnavailableError(slotToken, List.of());
        return dataSource.bookExperience(auth.guestId(), experienceId, slotToken,
                participants, (String) input.get("specialRequests"));
    }

    @DgsMutation
    public Object bookDining(@InputArgument Map<String, Object> input,
                               @InputArgument String idempotencyKey,
                               DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        String slotToken = (String) input.get("slotToken");
        Integer partySize = input.get("partySize") != null
                ? ((Number) input.get("partySize")).intValue() : null;
        String restaurantId = (String) input.get("restaurantId");
        if (restaurantId == null || slotToken == null || partySize == null || partySize <= 0) {
            return new ValidationError("INVALID_INPUT",
                    "restaurantId, slotToken, and positive partySize are required",
                    List.of());
        }
        if (!dataSource.isSlotValid(slotToken))
            return new SlotUnavailableError(slotToken, List.of());
        return dataSource.bookDining(auth.guestId(), restaurantId, slotToken,
                partySize, (String) input.get("specialRequests"));
    }

    @DgsMutation
    public Object bookGolfTeeTime(@InputArgument Map<String, Object> input,
                                    @InputArgument String idempotencyKey,
                                    DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        String courseId = (String) input.get("courseId");
        String slotToken = (String) input.get("slotToken");
        Integer players = input.get("players") != null
                ? ((Number) input.get("players")).intValue() : null;
        if (courseId == null || slotToken == null || players == null || players <= 0) {
            return new ValidationError("INVALID_INPUT",
                    "courseId, slotToken, and positive players are required",
                    List.of());
        }
        if (!dataSource.isSlotValid(slotToken))
            return new SlotUnavailableError(slotToken, List.of());
        Boolean cart = (Boolean) input.get("cartRequested");
        return dataSource.bookGolf(auth.guestId(), courseId, slotToken, players, cart);
    }

    @DgsMutation
    public Object cancelExperienceBooking(@InputArgument String bookingId,
                                            @InputArgument String reason,
                                            DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        if (dataSource.findBookingById(bookingId).isEmpty())
            return new NotFoundError("ExperienceBooking", bookingId);
        ExperienceBooking b = dataSource.cancel(bookingId, reason);
        return new CancelExperienceSuccess(
                b.getId(),
                null,
                b.getCancelledAt() != null ? b.getCancelledAt() : OffsetDateTime.now(),
                "Booking " + b.getConfirmationCode() + " cancelled");
    }

    // ── Federation field resolvers ────────────────────────────────────────────

    @DgsData(parentType = "Hotel", field = "experiences")
    public List<Experience> hotelExperiences(DataFetchingEnvironment dfe) {
        Hotel hotel = dfe.getSource();
        String category = dfe.getArgument("category");
        return dataSource.findExperiences(hotel.getId(), category, null);
    }

    @DgsData(parentType = "Hotel", field = "spaTreatments")
    public List<SpaTreatment> hotelSpaTreatments(DataFetchingEnvironment dfe) {
        Hotel hotel = dfe.getSource();
        return dataSource.spaTreatments(hotel.getId());
    }

    @DgsEntityFetcher(name = "Experience")
    public Experience fetchExperience(Map<String, Object> values) {
        return dataSource.findExperienceById((String) values.get("id")).orElse(null);
    }

    @DgsEntityFetcher(name = "ExperienceBooking")
    public ExperienceBooking fetchBooking(Map<String, Object> values) {
        return dataSource.findBookingById((String) values.get("id")).orElse(null);
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
