package com.luxe.reservations.resolver;

import com.luxe.reservations.schema.types.GuestProfile;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthRole;
import com.luxe.common.error.NotFoundError;
import com.luxe.common.pagination.Connection;
import com.luxe.reservations.datasource.ReservationDataSource;
import com.luxe.reservations.schema.types.*;
import com.netflix.graphql.dgs.*;
import graphql.schema.DataFetchingEnvironment;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@DgsComponent
public class ReservationDataFetcher {

    private final ReservationDataSource dataSource;

    public ReservationDataFetcher(ReservationDataSource dataSource) {
        this.dataSource = dataSource;
    }

    private AuthContext getAuth(DataFetchingEnvironment dfe) {
        try {
            HttpServletRequest req = dfe.getGraphQlContext().get(HttpServletRequest.class);
            if (req == null) return AuthContext.anonymous();
            AuthContext ctx = (AuthContext) req.getAttribute("authContext");
            return ctx != null ? ctx : AuthContext.anonymous();
        } catch (Exception e) { return AuthContext.anonymous(); }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @DgsQuery
    public Object reservation(@InputArgument String id, DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        return dataSource.findById(id).<Object>map(r -> r)
                .orElse(new NotFoundError("Reservation", id));
    }

    @DgsQuery
    public Object reservationByConfirmationNumber(@InputArgument String confirmationNumber,
                                                   @InputArgument String guestLastName) {
        return dataSource.findByConfirmationNumber(confirmationNumber, guestLastName)
                .<Object>map(r -> r)
                .orElse(new NotFoundError("Reservation", confirmationNumber));
    }

    @DgsQuery
    public Object myReservations(@InputArgument Integer first, @InputArgument String after,
                                  @InputArgument Map<String, Object> filter,
                                  @InputArgument String sortBy,
                                  DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        String status  = filter != null ? (String) filter.get("status") : null;
        String hotelId = filter != null ? (String) filter.get("hotelId") : null;
        List<Reservation> all = dataSource.findByGuestId(auth.guestId(), status, hotelId, sortBy);
        Connection<Reservation> conn = Connection.of(all, first != null ? first : 10, after);
        return Map.of(
                "edges", conn.edges().stream().map(e -> Map.of("node", e.node(), "cursor", e.cursor())).toList(),
                "pageInfo", Map.of("hasNextPage", conn.pageInfo().hasNextPage(),
                        "hasPreviousPage", conn.pageInfo().hasPreviousPage(),
                        "startCursor", conn.pageInfo().startCursor(),
                        "endCursor", conn.pageInfo().endCursor()),
                "totalCount", conn.totalCount());
    }

    @DgsQuery
    public Object checkInEligibility(@InputArgument String reservationId,
                                      DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        return dataSource.checkInEligibility(reservationId);
    }

    @DgsQuery
    public Object digitalKey(@InputArgument String reservationId, DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        return dataSource.findById(reservationId)
                .map(Reservation::getDigitalKey)
                .orElse(null);
    }

    @DgsQuery
    public List<DiningReservation> myDiningReservations(@InputArgument Boolean upcoming,
                                                         @InputArgument Integer first,
                                                         @InputArgument String after,
                                                         DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        List<DiningReservation> all = dataSource.findDiningByGuestId(auth.guestId(), upcoming);
        return first != null ? all.stream().limit(first).toList() : all;
    }

    @DgsQuery
    public List<SpaAppointment> mySpaAppointments(@InputArgument Boolean upcoming,
                                                   @InputArgument Integer first,
                                                   @InputArgument String after,
                                                   DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        List<SpaAppointment> all = dataSource.findSpaByGuestId(auth.guestId(), upcoming);
        return first != null ? all.stream().limit(first).toList() : all;
    }

    @DgsQuery
    public List<TransportationBooking> myTransportationBookings(@InputArgument Boolean upcoming,
                                                                 @InputArgument Integer first,
                                                                 DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        List<TransportationBooking> all = dataSource.findTransportByGuestId(auth.guestId(), upcoming);
        return first != null ? all.stream().limit(first).toList() : all;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @DgsMutation
    public Object createReservation(@InputArgument Map<String, Object> input,
                                     @InputArgument String idempotencyKey,
                                     DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        String checkInStr  = (String) input.get("checkIn");
        String checkOutStr = (String) input.get("checkOut");
        if (checkInStr == null || checkOutStr == null) {
            return new com.luxe.common.error.ValidationError("MISSING_DATES",
                    "checkIn and checkOut are required",
                    List.of(new com.luxe.common.error.FieldError("checkIn", "Required")));
        }
        java.time.LocalDate ci = java.time.LocalDate.parse(checkInStr);
        java.time.LocalDate co = java.time.LocalDate.parse(checkOutStr);
        if (!ci.isBefore(co)) {
            return new com.luxe.common.error.ValidationError("INVALID_DATES",
                    "checkOut must be after checkIn",
                    List.of(new com.luxe.common.error.FieldError("checkOut", "Must be after checkIn")));
        }
        return dataSource.create(input, auth.guestId());
    }

    @DgsMutation
    public Object confirmReservation(@InputArgument String reservationId,
                                      @InputArgument Map<String, Object> input,
                                      @InputArgument String idempotencyKey,
                                      DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        Reservation r = dataSource.findById(reservationId).orElse(null);
        if (r == null) return new NotFoundError("Reservation", reservationId);
        String paymentMethodId = (String) input.get("paymentMethodId");
        String guestProfileId  = (String) input.get("guestProfileId");
        return dataSource.confirm(reservationId, paymentMethodId, guestProfileId);
    }

    @DgsMutation
    public Object modifyReservation(@InputArgument String reservationId,
                                     @InputArgument Map<String, Object> input,
                                     @InputArgument String idempotencyKey,
                                     DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        if (dataSource.findById(reservationId).isEmpty())
            return new NotFoundError("Reservation", reservationId);
        return dataSource.modify(reservationId, input);
    }

    @DgsMutation
    public Object cancelReservation(@InputArgument String reservationId,
                                     @InputArgument Map<String, Object> input,
                                     @InputArgument String idempotencyKey,
                                     DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        if (dataSource.findById(reservationId).isEmpty())
            return new NotFoundError("Reservation", reservationId);
        String reason = input != null ? (String) input.get("reason") : null;
        boolean acceptPenalty = input != null && Boolean.TRUE.equals(input.get("acceptPenalty"));
        return dataSource.cancel(reservationId, reason, acceptPenalty);
    }

    @DgsMutation
    public Object mobileCheckIn(@InputArgument String reservationId,
                                  @InputArgument Map<String, Object> input,
                                  @InputArgument String idempotencyKey,
                                  DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        Reservation r = dataSource.findById(reservationId).orElse(null);
        if (r == null) return new NotFoundError("Reservation", reservationId);
        Reservation updated = dataSource.mobileCheckIn(reservationId, input);
        return Map.of("reservation", updated, "digitalKey", updated.getDigitalKey(),
                "message", "Mobile check-in successful. Your digital key is ready.");
    }

    @DgsMutation
    public Object expressCheckout(@InputArgument String reservationId,
                                   DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        Reservation r = dataSource.findById(reservationId).orElse(null);
        if (r == null) return new NotFoundError("Reservation", reservationId);
        Reservation updated = dataSource.expressCheckout(reservationId);
        return Map.of("reservationId", reservationId, "folio", updated.getFolio(),
                "emailedTo", "guest@email.com",
                "message", "Express checkout complete. Your folio has been emailed.");
    }

    @DgsMutation
    public Object requestRoomUpgrade(@InputArgument String reservationId,
                                      @InputArgument Map<String, Object> input,
                                      DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        if (dataSource.findById(reservationId).isEmpty())
            return new NotFoundError("Reservation", reservationId);
        Reservation updated = dataSource.requestUpgrade(reservationId,
                input != null ? input : Map.of());
        return updated.getRoomUpgradeRequest();
    }

    @DgsMutation
    public Object applyGiftCard(@InputArgument String reservationId,
                                 @InputArgument String giftCardCode,
                                 DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        if (dataSource.findById(reservationId).isEmpty())
            return new NotFoundError("Reservation", reservationId);
        Reservation updated = dataSource.applyGiftCard(reservationId, giftCardCode);
        return updated.getRateBreakdown();
    }

    @DgsMutation
    public Object createDiningReservation(@InputArgument Map<String, Object> input,
                                           @InputArgument String idempotencyKey,
                                           DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        if (input.get("reservationId") == null || input.get("restaurantId") == null)
            return new com.luxe.common.error.ValidationError("MISSING_FIELDS",
                    "reservationId and restaurantId are required",
                    List.of(new com.luxe.common.error.FieldError("reservationId", "Required")));
        return dataSource.createDining(input, auth.guestId());
    }

    @DgsMutation
    public Object bookSpaAppointment(@InputArgument Map<String, Object> input,
                                      @InputArgument String idempotencyKey,
                                      DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        return dataSource.bookSpa(input, auth.guestId());
    }

    @DgsMutation
    public Object bookTransportation(@InputArgument Map<String, Object> input,
                                      @InputArgument String idempotencyKey,
                                      DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        return dataSource.bookTransport(input, auth.guestId());
    }

    // ── GuestProfile field resolvers ──────────────────────────────────────────

    @DgsData(parentType = "GuestProfile", field = "reservations")
    public List<Reservation> guestReservations(DataFetchingEnvironment dfe) {
        GuestProfile guest = dfe.getSource();
        String guestId = guest.getId();
        Map<String, Object> filter = dfe.getArgument("filter");
        String status  = filter != null ? (String) filter.get("status") : null;
        String hotelId = filter != null ? (String) filter.get("hotelId") : null;
        return dataSource.findByGuestId(guestId, status, hotelId, null);
    }

    @DgsData(parentType = "GuestProfile", field = "upcomingReservation")
    public Reservation upcomingReservation(DataFetchingEnvironment dfe) {
        GuestProfile guest = dfe.getSource();
        String guestId = guest.getId();
        return dataSource.findByGuestId(guestId, "CONFIRMED", null, "CHECK_IN_ASC")
                .stream()
                .filter(r -> !r.getCheckIn().isBefore(java.time.LocalDate.now()))
                .findFirst().orElse(null);
    }

    // ── Entity Fetcher ────────────────────────────────────────────────────────

    @DgsEntityFetcher(name = "Reservation")
    public Reservation fetchReservation(Map<String, Object> values) {
        return dataSource.findById((String) values.get("id")).orElse(null);
    }
    @DgsEntityFetcher(name = "GuestProfile")
    public GuestProfile fetchGuestProfileReference(Map<String, Object> values) {
        return new GuestProfile((String) values.get("id"));
    }

}
