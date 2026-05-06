package com.luxe.reservations.datasource;

import com.luxe.reservations.schema.types.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationMockDataSourceTest {

    private ReservationMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new ReservationMockDataSource();
    }

    @Test
    void seeded_data_includes_known_reservations() {
        assertThat(ds.findById("res-001")).isPresent();
        assertThat(ds.findById("res-002")).isPresent();
    }

    @Test
    void find_by_id_returns_empty_for_unknown() {
        assertThat(ds.findById("does-not-exist")).isEmpty();
    }

    @Test
    void find_by_confirmation_number_round_trips() {
        Reservation any = ds.findById("res-001").orElseThrow();
        assertThat(ds.findByConfirmationNumber(any.getConfirmationNumber(), null))
                .isPresent().get().extracting(Reservation::getId).isEqualTo("res-001");
    }

    @Test
    void find_by_guest_id_returns_only_that_guests_reservations() {
        List<Reservation> mine = ds.findByGuestId("guest-001", null, null, null);
        assertThat(mine).isNotEmpty();
        assertThat(mine).allSatisfy(r -> assertThat(r.getGuestId()).isEqualTo("guest-001"));
    }

    @Test
    void find_by_guest_id_filters_by_status() {
        List<Reservation> confirmed = ds.findByGuestId("guest-001", "CONFIRMED", null, null);
        assertThat(confirmed).allSatisfy(r -> assertThat(r.getStatus()).isEqualTo("CONFIRMED"));
    }

    @Test
    void find_by_hotel_id_filters_to_that_hotel() {
        List<Reservation> here = ds.findByHotelId("prop-paris-001");
        assertThat(here).allSatisfy(r -> assertThat(r.getHotelId()).isEqualTo("prop-paris-001"));
    }

    @Test
    void check_in_eligibility_for_confirmed_near_term_reservation() {
        // res-002: London check-in in 3 days, CONFIRMED
        var elig = ds.checkInEligibility("res-002");
        assertThat(elig).isNotNull();
        assertThat(elig.reservationId()).isEqualTo("res-002");
    }

    @Test
    void check_in_eligibility_unknown_reservation_returns_ineligible() {
        var elig = ds.checkInEligibility("not-a-thing");
        assertThat(elig.eligible()).isFalse();
    }
}
