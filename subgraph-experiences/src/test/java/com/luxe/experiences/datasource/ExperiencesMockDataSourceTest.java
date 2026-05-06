package com.luxe.experiences.datasource;

import com.luxe.experiences.schema.types.Experience;
import com.luxe.experiences.schema.types.ExperienceAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExperiencesMockDataSourceTest {

    private ExperiencesMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new ExperiencesMockDataSource();
    }

    @Test
    void seeded_experiences_are_present() {
        List<Experience> all = ds.findExperiences(null, null, null);
        assertThat(all).isNotEmpty();
    }

    @Test
    void find_experiences_filters_by_hotel_id() {
        List<Experience> paris = ds.findExperiences("prop-paris-001", null, null);
        assertThat(paris).isNotEmpty();
        assertThat(paris).allSatisfy(e ->
                assertThat(e.hotelId()).isEqualTo("prop-paris-001"));
    }

    @Test
    void find_experiences_filters_by_category() {
        List<Experience> spa = ds.findExperiences("prop-paris-001", "SPA_WELLNESS", null);
        assertThat(spa).isNotEmpty();
        assertThat(spa).allSatisfy(e -> assertThat(e.category()).isEqualTo("SPA_WELLNESS"));
    }

    @Test
    void experience_by_id_resolves_when_present() {
        Experience first = ds.findExperiences(null, null, null).get(0);
        assertThat(ds.findExperienceById(first.id())).isPresent();
    }

    @Test
    void availability_returns_slots_for_known_experience() {
        Experience first = ds.findExperiences(null, null, null).get(0);
        ExperienceAvailability avail = ds.availability(first.id(), LocalDate.now().plusDays(3), 2);
        assertThat(avail).isNotNull();
        assertThat(avail.slots()).isNotEmpty();
        assertThat(avail.slots()).allSatisfy(s -> assertThat(s.slotToken()).isNotBlank());
    }

    @Test
    void availability_for_unknown_experience_marks_fully_booked() {
        ExperienceAvailability a = ds.availability("not-real", LocalDate.now().plusDays(3), 2);
        assertThat(a.fullyBooked()).isTrue();
    }

    @Test
    void issued_slot_token_validates_as_known_slot() {
        Experience first = ds.findExperiences(null, null, null).get(0);
        var token = ds.availability(first.id(), LocalDate.now().plusDays(3), 2)
                .slots().get(0).slotToken();
        assertThat(ds.isSlotValid(token)).isTrue();
    }

    @Test
    void invalid_slot_token_is_rejected() {
        assertThat(ds.isSlotValid(null)).isFalse();
        assertThat(ds.isSlotValid("garbage")).isFalse();
    }

    @Test
    void spa_treatments_for_hotel_returns_only_that_hotels_treatments() {
        var paris = ds.spaTreatments("prop-paris-001");
        assertThat(paris).isNotEmpty();
        assertThat(paris).allSatisfy(t -> assertThat(t.hotelId()).isEqualTo("prop-paris-001"));
    }
}
