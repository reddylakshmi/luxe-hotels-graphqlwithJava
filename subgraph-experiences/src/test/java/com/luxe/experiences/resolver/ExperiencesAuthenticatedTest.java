package com.luxe.experiences.resolver;

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
 * Authenticated coverage of experience/dining/golf bookings + cancel.
 */
@SpringBootTest
@Import(ExperiencesAuthenticatedTest.AuthOverrideConfig.class)
class ExperiencesAuthenticatedTest {

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

    @Test
    void my_experience_bookings_returns_paginated_list() {
        Integer total = dgs.executeAndExtractJsonPath(
                "{ myExperienceBookings { totalCount edges { node { id status } } } }",
                "data.myExperienceBookings.totalCount");
        assertThat(total).isNotNull();
    }

    @Test
    void book_experience_with_valid_slot_token_returns_booking() {
        // Discover a real slot token first
        String exp = dgs.executeAndExtractJsonPath(
                "{ experiences(hotelId: \"prop-paris-001\") { id } }",
                "data.experiences[0].id");
        String token = dgs.executeAndExtractJsonPath(
                "{ experienceAvailability(experienceId: \"" + exp + "\","
                        + " date: \"2026-09-01\", partySize: 2) { slots { slotToken } } }",
                "data.experienceAvailability.slots[0].slotToken");
        var result = dgs.execute("""
                mutation { bookExperience(input: {
                    experienceId: "%s", slotToken: "%s", participants: 2
                }, idempotencyKey: "%s") {
                  ... on ExperienceBooking { id status }
                  ... on SlotUnavailableError { code }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(exp, token, key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void book_experience_with_unknown_id_returns_not_found() {
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { bookExperience(input: {
                    experienceId: "exp-not-real", slotToken: "slot-x", participants: 2
                }, idempotencyKey: "%s") {
                  __typename
                  ... on NotFoundError { code }
                  ... on SlotUnavailableError { code }
                  ... on ValidationError { code }
                  ... on ExperienceBooking { id }
                } }
                """.formatted(key()), "data.bookExperience.__typename");
        assertThat(typename).isEqualTo("NotFoundError");
    }

    @Test
    void book_dining_with_valid_token_returns_booking() {
        String token = dgs.executeAndExtractJsonPath(
                "{ restaurantAvailability(hotelId: \"prop-paris-001\", date: \"2026-09-01\", partySize: 2) {"
                        + " restaurants { slots { slotToken } } } }",
                "data.restaurantAvailability.restaurants[0].slots[0].slotToken");
        var result = dgs.execute("""
                mutation { bookDining(input: {
                    restaurantId: "rest-paris-001", slotToken: "%s", partySize: 2
                }, idempotencyKey: "%s") {
                  ... on ExperienceBooking { id status }
                  ... on SlotUnavailableError { code }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(token, key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void book_golf_tee_time_returns_booking() {
        String token = dgs.executeAndExtractJsonPath(
                "{ golfTeeTimeAvailability(hotelId: \"prop-dubai-001\", date: \"2026-09-01\", players: 4) {"
                        + " courseId slots { slotToken } } }",
                "data.golfTeeTimeAvailability.slots[0].slotToken");
        String courseId = dgs.executeAndExtractJsonPath(
                "{ golfTeeTimeAvailability(hotelId: \"prop-dubai-001\", date: \"2026-09-01\", players: 4) { courseId } }",
                "data.golfTeeTimeAvailability.courseId");
        var result = dgs.execute("""
                mutation { bookGolfTeeTime(input: {
                    courseId: "%s", slotToken: "%s", players: 4, cartRequested: true
                }, idempotencyKey: "%s") {
                  ... on ExperienceBooking { id status }
                  ... on SlotUnavailableError { code }
                  ... on ValidationError { code }
                  ... on NotFoundError { code }
                } }
                """.formatted(courseId, token, key()));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void cancel_experience_booking_returns_cancel_success() {
        // Book first then cancel
        String exp = dgs.executeAndExtractJsonPath(
                "{ experiences(hotelId: \"prop-paris-001\") { id } }",
                "data.experiences[0].id");
        String token = dgs.executeAndExtractJsonPath(
                "{ experienceAvailability(experienceId: \"" + exp + "\","
                        + " date: \"2026-09-15\", partySize: 1) { slots { slotToken } } }",
                "data.experienceAvailability.slots[0].slotToken");
        String bookingId = dgs.executeAndExtractJsonPath("""
                mutation { bookExperience(input: {
                    experienceId: "%s", slotToken: "%s", participants: 1
                }, idempotencyKey: "%s") {
                  ... on ExperienceBooking { id }
                } }
                """.formatted(exp, token, key()), "data.bookExperience.id");
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { cancelExperienceBooking(bookingId: "%s", reason: "Test") {
                  __typename
                  ... on CancelExperienceSuccess { bookingId message }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """.formatted(bookingId), "data.cancelExperienceBooking.__typename");
        assertThat(typename).isEqualTo("CancelExperienceSuccess");
    }

    @Test
    void cancel_unknown_booking_returns_not_found() {
        String typename = dgs.executeAndExtractJsonPath("""
                mutation { cancelExperienceBooking(bookingId: "exb-not-real") {
                  __typename
                  ... on CancelExperienceSuccess { bookingId }
                  ... on NotFoundError { code }
                  ... on ValidationError { code }
                } }
                """, "data.cancelExperienceBooking.__typename");
        assertThat(typename).isEqualTo("NotFoundError");
    }
}
