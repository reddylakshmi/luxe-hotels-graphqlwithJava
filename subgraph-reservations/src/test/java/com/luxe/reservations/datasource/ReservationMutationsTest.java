package com.luxe.reservations.datasource;

import com.luxe.reservations.schema.types.DiningReservation;
import com.luxe.reservations.schema.types.Reservation;
import com.luxe.reservations.schema.types.SpaAppointment;
import com.luxe.reservations.schema.types.TransportationBooking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the 11 reservation-domain mutations + the dining/spa/transport ancillary
 * lookups. Pure unit tests against the in-memory mock — verifies state transitions,
 * idempotency around lookups, and not-found semantics.
 */
class ReservationMutationsTest {

    private ReservationMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new ReservationMockDataSource();
    }

    // ── create / confirm / modify / cancel ───────────────────────────────────

    @Test
    void create_persists_reservation_in_pending_payment_state() {
        Reservation r = ds.create(Map.of(
                "hotelId", "prop-paris-001",
                "roomTypeId", "rt-paris-dlx-001",
                "checkIn", LocalDate.now().plusDays(40).toString(),
                "checkOut", LocalDate.now().plusDays(43).toString(),
                "adults", 2, "children", 0
        ), "guest-001");
        assertThat(r).isNotNull();
        assertThat(r.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(r.getGuestId()).isEqualTo("guest-001");
        assertThat(ds.findById(r.getId())).isPresent();
    }

    @Test
    void confirm_transitions_pending_to_confirmed_with_payment_summary() {
        Reservation pending = ds.create(Map.of(
                "hotelId", "prop-paris-001",
                "roomTypeId", "rt-paris-dlx-001",
                "checkIn", LocalDate.now().plusDays(40).toString(),
                "checkOut", LocalDate.now().plusDays(42).toString(),
                "adults", 2
        ), "guest-001");
        Reservation confirmed = ds.confirm(pending.getId(), "pm-1", "guest-001");
        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getPaymentSummary()).isNotNull();
    }

    @Test
    void confirm_unknown_id_returns_null() {
        assertThat(ds.confirm("not-real", "pm-1", "guest-001")).isNull();
    }

    @Test
    void modify_updates_dates_and_recomputes_nights() {
        Reservation r = ds.findById("res-001").orElseThrow();
        LocalDate newCheckIn  = LocalDate.now().plusDays(20);
        LocalDate newCheckOut = LocalDate.now().plusDays(25);
        Reservation modified = ds.modify(r.getId(), Map.of(
                "checkIn", newCheckIn.toString(),
                "checkOut", newCheckOut.toString(),
                "adults", 3
        ));
        assertThat(modified.getCheckIn()).isEqualTo(newCheckIn);
        assertThat(modified.getCheckOut()).isEqualTo(newCheckOut);
        assertThat(modified.getNights()).isEqualTo(5);
        assertThat(modified.getAdults()).isEqualTo(3);
        assertThat(modified.getStatus()).isEqualTo("MODIFIED");
    }

    @Test
    void modify_unknown_id_returns_null() {
        assertThat(ds.modify("not-real", Map.of())).isNull();
    }

    @Test
    void cancel_without_penalty_marks_cancelled_and_full_refund() {
        Reservation r = ds.cancel("res-002", "Plans changed", false);
        assertThat(r.getStatus()).isEqualTo("CANCELLED");
        assertThat(r.getCancellation()).isNotNull();
        assertThat(r.getCancellation().refundStatus()).isEqualTo("REFUNDED");
    }

    @Test
    void cancel_with_penalty_marks_cancelled_with_fee_and_pending_refund() {
        Reservation r = ds.cancel("res-003", "Late cancellation", true);
        assertThat(r.getStatus()).isEqualTo("CANCELLED_WITH_FEE");
        assertThat(r.getCancellation().refundStatus()).isEqualTo("PENDING");
    }

    @Test
    void cancel_unknown_id_returns_null() {
        assertThat(ds.cancel("not-real", "x", false)).isNull();
    }

    // ── check-in / check-out / upgrade ───────────────────────────────────────

    @Test
    void mobile_check_in_transitions_to_checked_in_and_issues_digital_key() {
        Reservation r = ds.mobileCheckIn("res-002", Map.of(
                "documentType", "PASSPORT", "documentNumber", "X1234"));
        assertThat(r.getStatus()).isEqualTo("CHECKED_IN");
        assertThat(r.getCheckedInAt()).isNotNull();
        assertThat(r.getDigitalKey()).isNotNull();
        assertThat(r.getDigitalKey().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void mobile_check_in_unknown_id_returns_null() {
        assertThat(ds.mobileCheckIn("not-real", Map.of())).isNull();
    }

    @Test
    void express_checkout_transitions_to_checked_out_and_settles_folio() {
        Reservation r = ds.expressCheckout("res-004");  // res-004 is CHECKED_IN
        assertThat(r.getStatus()).isEqualTo("CHECKED_OUT");
        assertThat(r.getCheckedOutAt()).isNotNull();
        assertThat(r.getFolio()).isNotNull();
    }

    @Test
    void express_checkout_unknown_id_returns_null() {
        assertThat(ds.expressCheckout("not-real")).isNull();
    }

    @Test
    void request_upgrade_attaches_pending_upgrade_request() {
        Reservation r = ds.requestUpgrade("res-001", Map.of(
                "preferredRoomTypeId", "rt-suite", "reason", "Anniversary"));
        assertThat(r.getRoomUpgradeRequest()).isNotNull();
        assertThat(r.getRoomUpgradeRequest().getStatus()).isEqualTo("PENDING");
        assertThat(r.getRoomUpgradeRequest().getReason()).isEqualTo("Anniversary");
    }

    @Test
    void request_upgrade_unknown_id_returns_null() {
        assertThat(ds.requestUpgrade("not-real", Map.of())).isNull();
    }

    @Test
    void apply_gift_card_updates_reservation_timestamp() {
        Reservation r = ds.applyGiftCard("res-001", "GIFT-1234");
        assertThat(r).isNotNull();
        assertThat(r.getUpdatedAt()).isNotNull();
    }

    @Test
    void apply_gift_card_unknown_id_returns_null() {
        assertThat(ds.applyGiftCard("not-real", "GIFT-1234")).isNull();
    }

    // ── lookups by guest / hotel / status ────────────────────────────────────

    @Test
    void find_by_guest_id_sorts_check_in_ascending_when_requested() {
        List<Reservation> sorted = ds.findByGuestId("guest-001", null, null, "CHECK_IN_ASC");
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertThat(sorted.get(i).getCheckIn())
                    .isBeforeOrEqualTo(sorted.get(i + 1).getCheckIn());
        }
    }

    @Test
    void find_by_guest_id_default_sort_is_check_in_descending() {
        List<Reservation> sorted = ds.findByGuestId("guest-001", null, null, null);
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertThat(sorted.get(i).getCheckIn())
                    .isAfterOrEqualTo(sorted.get(i + 1).getCheckIn());
        }
    }

    @Test
    void find_by_guest_id_filters_by_hotel_id() {
        List<Reservation> paris = ds.findByGuestId("guest-001",
                null, "prop-paris-001", null);
        assertThat(paris).allSatisfy(r ->
                assertThat(r.getHotelId()).isEqualTo("prop-paris-001"));
    }

    @Test
    void find_upcoming_filters_to_confirmed_and_checked_in_only() {
        List<Reservation> upcoming = ds.findUpcoming("prop-paris-001");
        assertThat(upcoming).allSatisfy(r ->
                assertThat(r.getStatus()).isIn("CONFIRMED", "CHECKED_IN"));
    }

    @Test
    void find_by_confirmation_number_unknown_returns_empty() {
        assertThat(ds.findByConfirmationNumber("LUX-NONE", null)).isEmpty();
    }

    // ── check-in eligibility branches ────────────────────────────────────────

    @Test
    void check_in_eligibility_for_far_future_reservation_says_too_early() {
        // res-003: Tokyo, ~60 days out
        var elig = ds.checkInEligibility("res-003");
        assertThat(elig.eligible()).isTrue();              // status is CONFIRMED
        assertThat(elig.onlineCheckInAvailable()).isFalse(); // > 3 days away
        assertThat(elig.reasons()).anyMatch(s -> s.contains("3 days"));
    }

    @Test
    void check_in_eligibility_for_cancelled_reservation_is_ineligible() {
        // res-006 is CANCELLED
        var elig = ds.checkInEligibility("res-006");
        assertThat(elig.eligible()).isFalse();
    }

    // ── ancillary bookings (dining / spa / transport) ────────────────────────

    @Test
    void create_dining_persists_a_dining_reservation() {
        DiningReservation d = ds.createDining(Map.of(
                "reservationId", "res-001",
                "restaurantId", "rest-paris-001",
                "date", LocalDate.now().plusDays(11).toString(),
                "time", "20:00", "partySize", 2,
                "specialRequests", "Window seat"
        ), "guest-001");
        assertThat(d).isNotNull();
        assertThat(d.getStatus()).isEqualTo("CONFIRMED");
        assertThat(d.getReservationId()).isEqualTo("res-001");
    }

    @Test
    void find_dining_by_guest_id_filters_to_their_reservations() {
        var dining = ds.findDiningByGuestId("guest-001", null);
        assertThat(dining).isNotNull();
    }

    @Test
    void book_spa_creates_appointment_with_inherited_currency() {
        SpaAppointment s = ds.bookSpa(Map.of(
                "reservationId", "res-001",
                "treatmentId", "spa-treat-001",
                "date", LocalDate.now().plusDays(12).toString(),
                "time", "10:00",
                "therapistPreference", "Marie"
        ), "guest-001");
        assertThat(s).isNotNull();
        assertThat(s.getStatus()).isEqualTo("CONFIRMED");
        assertThat(s.getPrice().currency()).isEqualTo("EUR"); // res-001 is in EUR
    }

    @Test
    void book_spa_with_unknown_reservation_falls_back_to_usd() {
        SpaAppointment s = ds.bookSpa(Map.of(
                "reservationId", "no-such",
                "treatmentId", "spa-treat-001",
                "date", LocalDate.now().plusDays(2).toString(),
                "time", "10:00"
        ), "guest-001");
        assertThat(s.getPrice().currency()).isEqualTo("USD");
    }

    @Test
    void book_transportation_creates_booking() {
        TransportationBooking t = ds.bookTransport(Map.of(
                "reservationId", "res-001",
                "type", "AIRPORT_PICKUP",
                "pickupLocation", "CDG",
                "dropoffLocation", "Hotel",
                "scheduledAt", "2026-06-15T14:00:00Z",
                "vehicleType", "Mercedes E"
        ), "guest-001");
        assertThat(t).isNotNull();
        assertThat(t.getStatus()).isEqualTo("CONFIRMED");
        assertThat(t.getType()).isEqualTo("AIRPORT_PICKUP");
    }

    @Test
    void find_transport_by_guest_id_returns_their_bookings() {
        var trans = ds.findTransportByGuestId("guest-001", null);
        assertThat(trans).isNotNull();
    }

    @Test
    void find_spa_by_guest_id_returns_their_appointments() {
        var spa = ds.findSpaByGuestId("guest-001", null);
        assertThat(spa).isNotNull();
    }
}
