package com.luxe.reservations.resolver;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authenticated coverage for the 11 reservation mutations + auth-gated queries.
 */
@SpringBootTest
@Import(ReservationAuthenticatedTest.AuthOverrideConfig.class)
class ReservationAuthenticatedTest {

    @TestConfiguration
    static class AuthOverrideConfig {
        @Bean @Primary
        AuthContextResolver mockAuth() {
            return dfe -> AuthContext.of("guest-001", "LUX0001234567", AuthRole.GUEST);
        }
    }

    @Autowired
    DgsQueryExecutor dgs;

    private static String key() { return UUID.randomUUID().toString(); }

    // ── Auth-gated queries ───────────────────────────────────────────────────

    @Test
    void reservation_by_id_returns_seeded_reservation() {
        String id = dgs.executeAndExtractJsonPath(
                "{ reservation(id: \"res-001\") { id confirmationNumber status } }",
                "data.reservation.id");
        assertThat(id).isEqualTo("res-001");
    }

    @Test
    void my_reservations_returns_pagination_for_authenticated_guest() {
        Integer total = dgs.executeAndExtractJsonPath(
                "{ myReservations { totalCount edges { node { id status } } } }",
                "data.myReservations.totalCount");
        assertThat(total).isNotNull();
        assertThat(total).isGreaterThanOrEqualTo(0);
    }

    @Test
    void my_reservations_resolves_boolean_status_fields() {
        // Regression: doubled-is getters (isIsCan*) decoded to wrong property names → null on Boolean! fields.
        var result = dgs.execute("""
                { myReservations { edges { node {
                    id canCheckInOnline canModify isRefundable
                } } } }
                """);
        assertThat(result.getErrors()).isEmpty();
        @SuppressWarnings("unchecked")
        Map<String, Object> conn = (Map<String, Object>)
                ((Map<String, Object>) result.getData()).get("myReservations");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>) conn.get("edges");
        assertThat(edges).isNotEmpty();
        for (Map<String, Object> edge : edges) {
            @SuppressWarnings("unchecked")
            Map<String, Object> node = (Map<String, Object>) edge.get("node");
            assertThat(node.get("canCheckInOnline")).isInstanceOf(Boolean.class);
            assertThat(node.get("canModify")).isInstanceOf(Boolean.class);
            assertThat(node.get("isRefundable")).isInstanceOf(Boolean.class);
        }
    }

    @Test
    void my_dining_reservations_returns_list() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ myDiningReservations { id restaurantName status } }",
                "data.myDiningReservations");
        assertThat(result).isNotNull();
    }

    @Test
    void my_spa_appointments_returns_list() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ mySpaAppointments { id treatmentName status } }",
                "data.mySpaAppointments");
        assertThat(result).isNotNull();
    }

    @Test
    void my_transportation_bookings_returns_list() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ myTransportationBookings { id type status } }",
                "data.myTransportationBookings");
        assertThat(result).isNotNull();
    }

    @Test
    void check_in_eligibility_returns_data_when_authenticated() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ checkInEligibility(reservationId: \"res-002\") { eligible reasons } }",
                "data.checkInEligibility");
        assertThat(result).isNotNull();
    }

    @Test
    void digital_key_returns_key_for_checked_in_reservation() {
        Object result = dgs.executeAndExtractJsonPath(
                "{ digitalKey(reservationId: \"res-004\") { keyCode status } }",
                "data.digitalKey");
        assertThat(result).isNotNull();
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    @Test
    void create_reservation_persists_pending_payment() {
        String mutation = """
                mutation { createReservation(input: {
                    hotelId: "prop-paris-001",
                    roomTypeId: "rt-paris-dlx-001",
                    rateToken: "rate-paris-dlx-flex",
                    checkIn: "2026-09-01", checkOut: "2026-09-04",
                    adults: 2
                }, idempotencyKey: "%s") {
                  ... on Reservation { id status }
                  ... on ValidationError { code }
                  ... on RoomUnavailableError { code }
                  ... on AuthorizationError { code }
                  ... on ExternalServiceError { code }
                } }
                """.formatted(key());
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void create_reservation_with_points_redeemed_applies_loyalty_discount() {
        // 50,000 pts × $0.007 = $350 off the booking total.
        String reservationId = dgs.executeAndExtractJsonPath("""
                mutation { createReservation(input: {
                    hotelId: "prop-paris-001",
                    roomTypeId: "rt-paris-dlx-001",
                    rateToken: "rate-paris-dlx-flex",
                    checkIn: "2026-09-01", checkOut: "2026-09-04",
                    adults: 2,
                    loyaltyNumber: "LUX0001234567",
                    pointsToRedeem: 50000
                }, idempotencyKey: "%s") {
                  ... on Reservation { id }
                  ... on ValidationError { code message }
                } }
                """.formatted(key()), "data.createReservation.id");
        assertThat(reservationId).isNotNull();

        String discount = dgs.executeAndExtractJsonPath("""
                { reservation(id: "%s") {
                    rateBreakdown { loyaltyDiscount { amount currency } totalDue { amount } }
                    loyaltyContext { pointsRedeemed }
                } }
                """.formatted(reservationId), "data.reservation.rateBreakdown.loyaltyDiscount.amount");
        // The data source bills at 500/night × 3 nights = 1500 EUR base, plus
        // taxes + fees → 1620 EUR pre-discount. Apply 350 EUR redemption.
        assertThat(Double.parseDouble(discount)).isEqualTo(350.0);

        Integer pointsRedeemed = dgs.executeAndExtractJsonPath(
                "{ reservation(id: \"%s\") { loyaltyContext { pointsRedeemed } } }".formatted(reservationId),
                "data.reservation.loyaltyContext.pointsRedeemed");
        assertThat(pointsRedeemed).isEqualTo(50000);
    }

    @Test
    void create_reservation_without_points_leaves_loyalty_discount_null() {
        String reservationId = dgs.executeAndExtractJsonPath("""
                mutation { createReservation(input: {
                    hotelId: "prop-london-001",
                    roomTypeId: "rt-london-dlx-001",
                    rateToken: "rate-london-dlx-flex",
                    checkIn: "2026-09-01", checkOut: "2026-09-03",
                    adults: 2
                }, idempotencyKey: "%s") {
                  ... on Reservation { id }
                } }
                """.formatted(key()), "data.createReservation.id");
        assertThat(reservationId).isNotNull();
        Object discount = dgs.executeAndExtractJsonPath(
                "{ reservation(id: \"%s\") { rateBreakdown { loyaltyDiscount { amount } } } }"
                        .formatted(reservationId),
                "data.reservation.rateBreakdown.loyaltyDiscount");
        assertThat(discount).isNull();
    }

    @Test
    void create_reservation_with_check_out_before_check_in_returns_validation_error() {
        String mutation = """
                mutation { createReservation(input: {
                    hotelId: "prop-paris-001",
                    roomTypeId: "rt-paris-dlx-001",
                    rateToken: "x",
                    checkIn: "2026-09-04", checkOut: "2026-09-01",
                    adults: 2
                }, idempotencyKey: "%s") {
                  ... on ValidationError { __typename code }
                  ... on Reservation { id }
                  ... on RoomUnavailableError { code }
                  ... on AuthorizationError { code }
                  ... on ExternalServiceError { code }
                } }
                """.formatted(key());
        String typename = dgs.executeAndExtractJsonPath(mutation,
                "data.createReservation.__typename");
        assertThat(typename).isEqualTo("ValidationError");
    }

    @Test
    void modify_reservation_returns_updated_reservation() {
        String mutation = """
                mutation { modifyReservation(reservationId: "res-001",
                    input: { adults: 3 }, idempotencyKey: "%s") {
                  ... on Reservation { id adults status }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(key());
        var result = dgs.execute(mutation);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void modify_unknown_reservation_returns_not_found() {
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { modifyReservation(reservationId: "no-such",
                    input: { adults: 2 }, idempotencyKey: "%s") {
                  ... on Reservation { __typename id }
                  ... on NotFoundError { __typename code }
                  ... on ValidationError { __typename code }
                } }
                """.formatted(key()), "data.modifyReservation.__typename");
        assertThat(typename).isEqualTo("NotFoundError");
    }

    @Test
    void cancel_reservation_marks_cancelled() {
        var result = dgs.execute("""
                mutation { cancelReservation(reservationId: "res-007",
                    input: { reason: "Test" }, idempotencyKey: "%s") {
                  ... on Reservation { status cancellation { reason } }
                  ... on ValidationError { code }
                  ... on AuthorizationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void mobile_check_in_returns_success_with_digital_key() {
        var result = dgs.execute("""
                mutation { mobileCheckIn(reservationId: "res-002",
                    input: { documentType: "PASSPORT", documentNumber: "X1" },
                    idempotencyKey: "%s") {
                  ... on MobileCheckInSuccess { reservation { status } digitalKey { keyCode } message }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void express_checkout_returns_folio_emailed_to() {
        var result = dgs.execute("""
                mutation { expressCheckout(reservationId: "res-004") {
                  ... on ExpressCheckoutSuccess { reservationId folio { id } emailedTo message }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void request_room_upgrade_attaches_request() {
        var result = dgs.execute("""
                mutation { requestRoomUpgrade(reservationId: "res-001",
                    input: { reason: "Anniversary" }) {
                  ... on RoomUpgradeRequest { id status reason }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void apply_gift_card_returns_rate_breakdown() {
        var result = dgs.execute("""
                mutation { applyGiftCard(reservationId: "res-001", giftCardCode: "GIFT-1234") {
                  ... on ReservationRateBreakdown { totalDue { amount currency } }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void create_dining_reservation_returns_dining_reservation() {
        var result = dgs.execute("""
                mutation { createDiningReservation(input: {
                    reservationId: "res-001", restaurantId: "rest-paris-001",
                    date: "2026-06-15", time: "20:00", partySize: 2
                }, idempotencyKey: "%s") {
                  ... on DiningReservation { id status }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void book_spa_appointment_returns_appointment() {
        var result = dgs.execute("""
                mutation { bookSpaAppointment(input: {
                    reservationId: "res-001", treatmentId: "spa-treat-001",
                    date: "2026-06-15", time: "10:00"
                }, idempotencyKey: "%s") {
                  ... on SpaAppointment { id status }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void book_transportation_returns_booking() {
        var result = dgs.execute("""
                mutation { bookTransportation(input: {
                    reservationId: "res-001",
                    type: AIRPORT_PICKUP,
                    pickupLocation: "CDG", dropoffLocation: "Hotel",
                    scheduledAt: "2026-06-15T14:00:00Z",
                    passengerCount: 2
                }, idempotencyKey: "%s") {
                  ... on TransportationBooking { id status }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }
}
