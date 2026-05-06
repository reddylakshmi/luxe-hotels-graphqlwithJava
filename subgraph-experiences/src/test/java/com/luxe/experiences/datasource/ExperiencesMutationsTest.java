package com.luxe.experiences.datasource;

import com.luxe.experiences.schema.types.ExperienceBooking;
import com.luxe.experiences.schema.types.GolfTeeTimeAvailability;
import com.luxe.experiences.schema.types.RestaurantAvailability;
import com.luxe.experiences.schema.types.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises booking/cancel paths plus restaurant/golf availability and the
 * slot-token decode branches that drive instruction & branch coverage.
 */
class ExperiencesMutationsTest {

    private ExperiencesMockDataSource ds;
    private final LocalDate date = LocalDate.now().plusDays(7);

    @BeforeEach
    void setUp() {
        ds = new ExperiencesMockDataSource();
    }

    // ── bookings: experience / dining / golf ─────────────────────────────────

    @Test
    void book_experience_creates_a_confirmed_booking() {
        var exp = ds.findExperiences("prop-paris-001", null, null).get(0);
        TimeSlot slot = ds.availability(exp.id(), date, 2).slots().get(0);
        ExperienceBooking b = ds.bookExperience("guest-001", exp.id(),
                slot.slotToken(), 2, "Window seating");
        assertThat(b).isNotNull();
        assertThat(b.getStatus()).isEqualTo("CONFIRMED");
        assertThat(b.getGuestId()).isEqualTo("guest-001");
        assertThat(b.getExperienceId()).isEqualTo(exp.id());
        assertThat(b.getConfirmationCode()).startsWith("EXB-");
    }

    @Test
    void book_experience_for_unknown_experience_returns_null() {
        assertThat(ds.bookExperience("g1", "not-real", "slot-x", 2, null)).isNull();
    }

    @Test
    void book_experience_persists_total_price_in_experience_currency() {
        var exp = ds.findExperiences("prop-paris-001", null, null).get(0);
        TimeSlot slot = ds.availability(exp.id(), date, 2).slots().get(0);
        ExperienceBooking b = ds.bookExperience("g1", exp.id(), slot.slotToken(), 3, null);
        assertThat(b.getTotalPrice().currency()).isEqualTo(exp.currency());
    }

    @Test
    void book_dining_creates_booking_under_dining_prefix_id() {
        // Use an existing slot token from real availability for clean decode
        RestaurantAvailability avail = ds.restaurantAvailability("prop-paris-001", date, 2);
        TimeSlot slot = avail.restaurants().get(0).slots().get(0);
        ExperienceBooking b = ds.bookDining("g1", "rest-paris-001",
                slot.slotToken(), 2, "Birthday celebration");
        assertThat(b).isNotNull();
        assertThat(b.getExperienceId()).startsWith("dining-");
        assertThat(b.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void book_dining_unknown_restaurant_falls_back_to_unknown_hotel() {
        RestaurantAvailability avail = ds.restaurantAvailability("prop-paris-001", date, 2);
        TimeSlot slot = avail.restaurants().get(0).slots().get(0);
        ExperienceBooking b = ds.bookDining("g1", "rest-not-real",
                slot.slotToken(), 2, null);
        assertThat(b).isNotNull();
        assertThat(b.getHotelId()).isEqualTo("unknown");
    }

    @Test
    void book_golf_with_cart_requested_records_note() {
        GolfTeeTimeAvailability avail = ds.golfAvailability("prop-dubai-001", date, 4);
        TimeSlot slot = avail.slots().get(0);
        ExperienceBooking b = ds.bookGolf("g1", avail.courseId(), slot.slotToken(), 4, true);
        assertThat(b).isNotNull();
        assertThat(b.getSpecialRequests()).contains("Cart");
    }

    @Test
    void book_golf_without_cart_request_has_null_note() {
        GolfTeeTimeAvailability avail = ds.golfAvailability("prop-dubai-001", date, 4);
        TimeSlot slot = avail.slots().get(0);
        ExperienceBooking b = ds.bookGolf("g1", avail.courseId(), slot.slotToken(), 4, false);
        assertThat(b.getSpecialRequests()).isNull();
    }

    @Test
    void book_golf_unknown_course_falls_back_to_unknown_hotel() {
        // generate a token from a real course so decode succeeds
        GolfTeeTimeAvailability avail = ds.golfAvailability("prop-dubai-001", date, 4);
        ExperienceBooking b = ds.bookGolf("g1", "course-not-real",
                avail.slots().get(0).slotToken(), 4, null);
        assertThat(b.getHotelId()).isEqualTo("unknown");
    }

    // ── cancellation ─────────────────────────────────────────────────────────

    @Test
    void cancel_existing_booking_marks_cancelled_with_reason() {
        var exp = ds.findExperiences("prop-paris-001", null, null).get(0);
        TimeSlot slot = ds.availability(exp.id(), date, 2).slots().get(0);
        ExperienceBooking booked = ds.bookExperience("g1", exp.id(), slot.slotToken(), 2, null);
        ExperienceBooking cancelled = ds.cancel(booked.getId(), "Schedule conflict");
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getCancellationReason()).isEqualTo("Schedule conflict");
    }

    @Test
    void cancel_unknown_booking_returns_null() {
        assertThat(ds.cancel("not-real", "reason")).isNull();
    }

    // ── booking lookup queries ───────────────────────────────────────────────

    @Test
    void find_bookings_by_guest_id_returns_their_bookings() {
        var exp = ds.findExperiences("prop-paris-001", null, null).get(0);
        TimeSlot slot = ds.availability(exp.id(), date, 2).slots().get(0);
        ds.bookExperience("guest-007", exp.id(), slot.slotToken(), 1, null);
        List<ExperienceBooking> mine = ds.findBookingsByGuestId("guest-007", null);
        assertThat(mine).isNotEmpty();
        assertThat(mine).allSatisfy(b -> assertThat(b.getGuestId()).isEqualTo("guest-007"));
    }

    @Test
    void find_bookings_by_guest_id_with_upcoming_filter_returns_future_only() {
        var exp = ds.findExperiences("prop-paris-001", null, null).get(0);
        TimeSlot slot = ds.availability(exp.id(), date, 2).slots().get(0);
        ds.bookExperience("guest-007", exp.id(), slot.slotToken(), 1, null);
        List<ExperienceBooking> upcoming = ds.findBookingsByGuestId("guest-007", true);
        assertThat(upcoming).allSatisfy(b ->
                assertThat(b.getDate()).isAfterOrEqualTo(LocalDate.now()));
    }

    @Test
    void find_booking_by_id_resolves_after_create() {
        var exp = ds.findExperiences("prop-paris-001", null, null).get(0);
        TimeSlot slot = ds.availability(exp.id(), date, 2).slots().get(0);
        ExperienceBooking b = ds.bookExperience("g1", exp.id(), slot.slotToken(), 1, null);
        assertThat(ds.findBookingById(b.getId())).isPresent();
    }

    @Test
    void find_booking_by_unknown_id_is_empty() {
        assertThat(ds.findBookingById("not-real")).isEmpty();
    }

    // ── availability branches ───────────────────────────────────────────────

    @Test
    void restaurant_availability_returns_per_restaurant_slot_groups() {
        RestaurantAvailability avail = ds.restaurantAvailability("prop-paris-001", date, 2);
        assertThat(avail.restaurants()).isNotEmpty();
        assertThat(avail.restaurants()).allSatisfy(rs -> {
            assertThat(rs.restaurantName()).isNotBlank();
            assertThat(rs.slots()).isNotEmpty();
        });
    }

    @Test
    void restaurant_availability_for_unknown_hotel_returns_no_groups() {
        RestaurantAvailability avail = ds.restaurantAvailability("unknown-hotel", date, 2);
        assertThat(avail.restaurants()).isEmpty();
    }

    @Test
    void golf_availability_returns_slots_for_known_course() {
        GolfTeeTimeAvailability avail = ds.golfAvailability("prop-dubai-001", date, 4);
        assertThat(avail.courseId()).isNotBlank();
        assertThat(avail.slots()).isNotEmpty();
    }

    @Test
    void golf_availability_for_hotel_without_course_uses_default_course_id() {
        GolfTeeTimeAvailability avail = ds.golfAvailability("prop-paris-001", date, 4);
        // Paris doesn't have a seeded course; falls back to "course-default"
        assertThat(avail.courseId()).isEqualTo("course-default");
    }

    @Test
    void availability_known_experience_returns_unique_slot_tokens() {
        var exp = ds.findExperiences("prop-paris-001", null, null).get(0);
        var slots = ds.availability(exp.id(), date, 2).slots();
        long unique = slots.stream().map(TimeSlot::slotToken).distinct().count();
        assertThat(unique).isEqualTo(slots.size());
    }

    @Test
    void slot_token_starts_with_slot_prefix_to_be_accepted_by_validator() {
        var exp = ds.findExperiences("prop-paris-001", null, null).get(0);
        var slot = ds.availability(exp.id(), date, 2).slots().get(0);
        assertThat(slot.slotToken()).startsWith("slot-");
        assertThat(ds.isSlotValid(slot.slotToken())).isTrue();
    }

    @Test
    void unknown_slot_with_slot_prefix_is_still_accepted_by_validator() {
        // The validator is permissive: any slot- prefixed token passes.
        // This documents that contract — used by tests that book without first looking up availability.
        assertThat(ds.isSlotValid("slot-fabricated-token")).isTrue();
    }

    // ── experience filters ───────────────────────────────────────────────────

    @Test
    void find_experiences_with_no_hotel_filter_returns_all_available() {
        var all = ds.findExperiences(null, null, null);
        assertThat(all).isNotEmpty();
        assertThat(all).allSatisfy(e -> assertThat(e.available()).isTrue());
    }

    @Test
    void find_experience_by_unknown_id_is_empty() {
        assertThat(ds.findExperienceById("exp-not-real")).isEmpty();
    }

    @Test
    void spa_treatments_for_unknown_hotel_returns_empty() {
        // current impl filters by hotelId, so unknown returns empty list
        assertThat(ds.spaTreatments("unknown-hotel")).isEmpty();
    }

    @Test
    void spa_treatments_with_null_hotel_returns_all_treatments() {
        assertThat(ds.spaTreatments(null)).isNotEmpty();
    }
}
