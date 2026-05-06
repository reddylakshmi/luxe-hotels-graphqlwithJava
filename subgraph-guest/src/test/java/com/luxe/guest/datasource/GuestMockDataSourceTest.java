package com.luxe.guest.datasource;

import com.luxe.guest.schema.types.GuestProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GuestMockDataSourceTest {

    private GuestMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new GuestMockDataSource();
    }

    @Test
    void seeded_guests_include_known_ids() {
        List<GuestProfile> all = ds.findAll();
        assertThat(all).extracting(GuestProfile::getId).contains("guest-001");
    }

    @Test
    void find_by_id_returns_guest_when_present() {
        assertThat(ds.findById("guest-001")).isPresent();
    }

    @Test
    void find_by_id_returns_empty_when_missing() {
        assertThat(ds.findById("not-a-guest")).isEmpty();
    }

    @Test
    void find_by_email_round_trips_with_seeded_data() {
        GuestProfile any = ds.findAll().get(0);
        assertThat(ds.findByEmail(any.getEmail()))
                .isPresent().get()
                .extracting(GuestProfile::getId).isEqualTo(any.getId());
    }

    @Test
    void exists_by_email_is_true_for_seeded_email() {
        String email = ds.findAll().get(0).getEmail();
        assertThat(ds.existsByEmail(email)).isTrue();
    }

    @Test
    void exists_by_email_is_false_for_unknown_email() {
        assertThat(ds.existsByEmail("ghost@nowhere.example")).isFalse();
    }

    @Test
    void sign_in_with_valid_email_returns_auth_payload() {
        String email = ds.findAll().get(0).getEmail();
        var payload = ds.signIn(email, "any-password");
        assertThat(payload).isNotNull();
        assertThat(payload.accessToken()).isNotBlank();
    }

    @Test
    void sign_up_creates_a_new_guest_profile_with_token() {
        var payload = ds.signUp("brand-new@example.com", "pw", "Brand", "New", "+1-415-555-9999");
        assertThat(payload.accessToken()).isNotBlank();
        assertThat(ds.existsByEmail("brand-new@example.com")).isTrue();
    }

    @Test
    void save_hotel_then_unsave_round_trips() {
        String guestId = ds.findAll().get(0).getId();
        ds.saveHotel(guestId, "prop-paris-001");
        ds.unsaveHotel(guestId, "prop-paris-001");
        // operation should not throw or corrupt state
        assertThat(ds.findById(guestId)).isPresent();
    }

    @Test
    void add_travel_companion_returns_updated_profile() {
        String guestId = ds.findAll().get(0).getId();
        Map<String, Object> input = Map.of(
                "name", Map.of("firstName", "Jane", "lastName", "Doe"),
                "relationship", "SPOUSE",
                "email", "jane@example.com");
        GuestProfile updated = ds.addTravelCompanion(guestId, input);
        assertThat(updated).isNotNull();
    }
}
