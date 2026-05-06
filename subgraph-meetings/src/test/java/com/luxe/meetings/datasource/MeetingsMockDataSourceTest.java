package com.luxe.meetings.datasource;

import com.luxe.meetings.schema.types.CateringMenu;
import com.luxe.meetings.schema.types.EventSpace;
import com.luxe.meetings.schema.types.RFP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingsMockDataSourceTest {

    private MeetingsMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new MeetingsMockDataSource();
    }

    @Test
    void event_spaces_for_hotel_returns_only_that_hotels_spaces() {
        List<EventSpace> paris = ds.findSpacesByHotel("prop-paris-001", null);
        assertThat(paris).isNotEmpty();
        assertThat(paris).allSatisfy(s -> assertThat(s.hotelId()).isEqualTo("prop-paris-001"));
    }

    @Test
    void event_spaces_filter_by_min_capacity() {
        List<EventSpace> big = ds.findSpacesByHotel("prop-paris-001",
                Map.of("minCapacity", 400));
        assertThat(big).allSatisfy(s ->
                assertThat(s.capacityStyles()).anyMatch(c -> c.capacity() >= 400));
    }

    @Test
    void search_spaces_filters_by_attendees_and_setup() {
        List<EventSpace> hits = ds.searchSpaces(Map.of(
                "attendees", 200, "setup", "THEATER"));
        assertThat(hits).allSatisfy(s ->
                assertThat(s.capacityStyles()).anyMatch(c ->
                        "THEATER".equals(c.setup()) && c.capacity() >= 200));
    }

    @Test
    void event_space_by_id_returns_known_space() {
        EventSpace any = ds.findSpacesByHotel("prop-paris-001", null).get(0);
        assertThat(ds.findSpaceById(any.id())).isPresent();
    }

    @Test
    void rfp_by_id_returns_seeded_rfp() {
        assertThat(ds.findRFPById("rfp-001")).isPresent();
    }

    @Test
    void rfp_by_unknown_id_is_empty() {
        assertThat(ds.findRFPById("not-real")).isEmpty();
    }

    @Test
    void rfps_by_organizer_filter_to_that_email() {
        RFP r1 = ds.findRFPById("rfp-001").orElseThrow();
        List<RFP> mine = ds.findRFPsByOrganizer(r1.getContactEmail(), null);
        assertThat(mine).isNotEmpty();
        assertThat(mine).allSatisfy(r ->
                assertThat(r.getContactEmail()).isEqualTo(r1.getContactEmail()));
    }

    @Test
    void cancel_rfp_marks_status_cancelled() {
        ds.cancelRFP("rfp-002", "Plan changed", "tester");
        assertThat(ds.findRFPById("rfp-002").orElseThrow().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void catering_menus_for_hotel_returns_seeded_menus() {
        List<CateringMenu> menus = ds.findCateringMenus("prop-paris-001");
        assertThat(menus).isNotEmpty();
    }

    @Test
    void create_group_block_returns_block_with_remaining_rooms() {
        var block = ds.createGroupBlock(Map.of(
                "rfpId", "rfp-001",
                "hotelId", "prop-paris-001",
                "startDate", "2026-08-01",
                "endDate", "2026-08-05",
                "rooms", 50,
                "rate", 320.0,
                "currency", "EUR",
                "cutoffDate", "2026-07-15"));
        assertThat(block).isNotNull();
        assertThat(block.getTotalRooms()).isEqualTo(50);
        assertThat(block.getRemainingRooms()).isEqualTo(50);
    }
}
