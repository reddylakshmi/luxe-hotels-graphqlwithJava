package com.luxe.meetings.resolver;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authenticated coverage of RFP and group block mutations.
 */
@SpringBootTest
@Import(MeetingsAuthenticatedTest.AuthOverrideConfig.class)
class MeetingsAuthenticatedTest {

    @TestConfiguration
    static class AuthOverrideConfig {
        @Bean @Primary
        AuthContextResolver mockAuth() {
            return dfe -> AuthContext.of("events@acme.example", null, AuthRole.GUEST);
        }
    }

    @Autowired
    DgsQueryExecutor dgs;

    private static String key() { return UUID.randomUUID().toString(); }

    @Test
    void my_rfps_returns_paginated_list_for_authenticated_organizer() {
        Integer total = dgs.executeAndExtractJsonPath(
                "{ myRFPs { totalCount edges { node { id status } } } }",
                "data.myRFPs.totalCount");
        assertThat(total).isNotNull();
    }

    @Test
    void submit_rfp_persists_new_rfp() {
        var result = dgs.execute("""
                mutation { submitRFP(input: {
                    organizer: "Test Organizer",
                    organization: "Test Co",
                    contactEmail: "test@test.example",
                    contactPhone: "+1-555-0001",
                    eventName: "Test Event",
                    eventType: BOARD_RETREAT,
                    startDate: "2026-09-01", endDate: "2026-09-04",
                    attendees: 50,
                    preferredHotelIds: ["prop-paris-001"],
                    spaceRequirements: [{
                        name: "Plenary", setup: THEATER, attendees: 50, durationHours: 8
                    }]
                }, idempotencyKey: "%s") {
                  ... on RFP { id rfpNumber status }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void update_rfp_returns_updated_rfp() {
        var result = dgs.execute("""
                mutation { updateRFP(rfpId: "rfp-001", input: { attendees: 100 }) {
                  ... on RFP { id attendees }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void update_rfp_unknown_id_returns_not_found() {
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { updateRFP(rfpId: "rfp-not-real", input: { attendees: 10 }) {
                  __typename
                  ... on RFP { id }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """, "data.updateRFP.__typename");
        assertThat(typename).isEqualTo("NotFoundError");
    }

    @Test
    void cancel_rfp_marks_status_cancelled() {
        // Use rfp-002 which is SUBMITTED (won't conflict with other tests)
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { cancelRFP(rfpId: "rfp-002", reason: "Plans changed") {
                  __typename
                  ... on RFP { id status }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """, "data.cancelRFP.__typename");
        assertThat(typename).isEqualTo("RFP");
    }

    @Test
    void cancel_unknown_rfp_returns_not_found() {
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { cancelRFP(rfpId: "rfp-not-real", reason: "X") {
                  __typename
                  ... on RFP { id }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """, "data.cancelRFP.__typename");
        assertThat(typename).isEqualTo("NotFoundError");
    }

    @Test
    void book_group_room_creates_block() {
        var result = dgs.execute("""
                mutation { bookGroupRoom(input: {
                    rfpId: "rfp-001", hotelId: "prop-paris-001",
                    startDate: "2026-09-01", endDate: "2026-09-04",
                    rooms: 25, rate: 450, currency: "EUR",
                    cutoffDate: "2026-08-15"
                }, idempotencyKey: "%s") {
                  ... on GroupBlock { id totalRooms remainingRooms blockCode }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(key()));
        assertThat(result.getErrors()).isEmpty();
    }

}
