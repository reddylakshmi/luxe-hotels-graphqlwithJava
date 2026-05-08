package com.luxe.guest.datasource;

import com.luxe.guest.schema.types.AuthPayload;
import com.luxe.guest.schema.types.GuestPreferences;
import com.luxe.guest.schema.types.GuestProfile;
import com.luxe.guest.schema.types.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the guest data source's mutation paths and filter branches.
 * Pure unit tests against the in-memory mock; verifies state transitions
 * and the not-found / already-exists branches that drag branch coverage.
 */
class GuestMutationsTest {

    private GuestMockDataSource ds;
    private String existingGuestId;

    @BeforeEach
    void setUp() {
        ds = new GuestMockDataSource();
        existingGuestId = ds.findAll().get(0).getId();
    }

    // ── signIn / signUp ──────────────────────────────────────────────────────

    @Test
    void sign_in_with_unknown_email_throws_invalid_credentials() {
        assertThatThrownBy(() -> ds.signIn("nobody@nowhere.example", "pw"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void sign_in_token_format_uses_mock_jwt_prefix() {
        String email = ds.findAll().get(0).getEmail();
        AuthPayload p = ds.signIn(email, "any");
        assertThat(p.accessToken()).startsWith("mock-jwt-");
        assertThat(p.tokenType()).isEqualTo("Bearer");
        assertThat(p.expiresIn()).isEqualTo(3600);
        assertThat(p.isNewAccount()).isFalse();
    }

    @Test
    void sign_up_marks_is_new_account_true() {
        AuthPayload p = ds.signUp("new-flag@example.com", "pw", "F", "L", "+1-415-555-0001");
        assertThat(p.isNewAccount()).isTrue();
        assertThat(p.guest().getEmail()).isEqualTo("new-flag@example.com");
    }

    @Test
    void sign_up_with_existing_email_throws() {
        String existing = ds.findAll().get(0).getEmail();
        assertThatThrownBy(() -> ds.signUp(existing, "pw", "X", "Y", "+1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void sign_up_persists_so_signin_works_immediately_after() {
        ds.signUp("roundtrip@example.com", "pw", "Round", "Trip", "+1");
        assertThat(ds.signIn("roundtrip@example.com", "pw")).isNotNull();
    }

    // ── findByFilter branches ────────────────────────────────────────────────

    @Test
    void find_by_filter_email_substring_matches() {
        GuestProfile any = ds.findAll().get(0);
        String fragment = any.getEmail().substring(0, 3);
        List<GuestProfile> matches = ds.findByFilter(fragment, null, null);
        assertThat(matches).isNotEmpty();
        assertThat(matches).anyMatch(g -> g.getEmail().equals(any.getEmail()));
    }

    @Test
    void find_by_filter_with_null_args_returns_all() {
        assertThat(ds.findByFilter(null, null, null))
                .hasSize(ds.findAll().size());
    }

    // ── update / preferences ─────────────────────────────────────────────────

    @Test
    void update_returns_existing_guest() {
        assertThat(ds.update(existingGuestId, Map.of("phone", "+1"))).isNotNull();
    }

    @Test
    void update_unknown_guest_returns_null() {
        assertThat(ds.update("not-real", Map.of())).isNull();
    }

    @Test
    void update_preferences_replaces_preferences() {
        GuestPreferences fresh = GuestPreferences.defaults();
        GuestProfile g = ds.updatePreferences(existingGuestId, fresh);
        assertThat(g).isNotNull();
        assertThat(g.getPreferences()).isSameAs(fresh);
    }

    @Test
    void update_preferences_unknown_guest_returns_null() {
        assertThat(ds.updatePreferences("not-real", GuestPreferences.defaults()))
                .isNull();
    }

    // ── payment methods ──────────────────────────────────────────────────────

    @Test
    void add_payment_method_appends_to_guest() {
        GuestProfile before = ds.findById(existingGuestId).orElseThrow();
        int beforeCount = before.getPaymentMethods().size();
        ds.addPaymentMethod(existingGuestId, Map.of(
                "type", "CREDIT_CARD", "brand", "Visa", "lastFour", "4242",
                "holderName", "X X", "expiryMonth", 12, "expiryYear", 2030));
        assertThat(before.getPaymentMethods()).hasSize(beforeCount + 1);
    }

    @Test
    void add_payment_method_with_default_flag_clears_others() {
        ds.addPaymentMethod(existingGuestId, Map.of(
                "type", "CREDIT_CARD", "lastFour", "0001", "setAsDefault", true));
        ds.addPaymentMethod(existingGuestId, Map.of(
                "type", "CREDIT_CARD", "lastFour", "0002", "setAsDefault", true));
        GuestProfile g = ds.findById(existingGuestId).orElseThrow();
        long defaults = g.getPaymentMethods().stream().filter(PaymentMethod::isIsDefault).count();
        assertThat(defaults).isEqualTo(1L);
    }

    @Test
    void add_payment_method_unknown_guest_returns_null() {
        assertThat(ds.addPaymentMethod("not-real", Map.of("type", "X"))).isNull();
    }

    @Test
    void remove_payment_method_removes_by_id() {
        ds.addPaymentMethod(existingGuestId, Map.of("type", "CREDIT_CARD", "lastFour", "9999"));
        GuestProfile g = ds.findById(existingGuestId).orElseThrow();
        String pmId = g.getPaymentMethods().stream()
                .filter(pm -> "9999".equals(pm.getLastFour()))
                .findFirst().orElseThrow().getId();
        ds.removePaymentMethod(existingGuestId, pmId);
        assertThat(g.getPaymentMethods()).noneMatch(pm -> pm.getId().equals(pmId));
    }

    @Test
    void remove_payment_method_unknown_guest_returns_null() {
        assertThat(ds.removePaymentMethod("not-real", "pm-1")).isNull();
    }

    @Test
    void set_default_payment_method_promotes_specified_id() {
        ds.addPaymentMethod(existingGuestId, Map.of("lastFour", "1111"));
        GuestProfile g = ds.findById(existingGuestId).orElseThrow();
        String pmId = g.getPaymentMethods().stream()
                .filter(pm -> "1111".equals(pm.getLastFour()))
                .findFirst().orElseThrow().getId();
        ds.setDefaultPaymentMethod(existingGuestId, pmId);
        long defaults = g.getPaymentMethods().stream().filter(PaymentMethod::isIsDefault).count();
        assertThat(defaults).isEqualTo(1L);
        assertThat(g.getPaymentMethods().stream().filter(PaymentMethod::isIsDefault).findFirst()
                .orElseThrow().getId()).isEqualTo(pmId);
    }

    @Test
    void set_default_payment_method_unknown_guest_returns_null() {
        assertThat(ds.setDefaultPaymentMethod("not-real", "pm-1")).isNull();
    }

    // ── saved hotels ─────────────────────────────────────────────────────────

    @Test
    void save_hotel_appends_when_not_already_saved() {
        GuestProfile g = ds.findById(existingGuestId).orElseThrow();
        int before = g.getSavedHotels().size();
        String fresh = pickUnsavedHotel(g);
        ds.saveHotel(existingGuestId, fresh);
        assertThat(g.getSavedHotels()).hasSize(before + 1);
    }

    private static String pickUnsavedHotel(GuestProfile g) {
        for (String candidate : List.of("prop-nyc-001", "prop-london-001",
                "prop-dubai-001", "prop-tokyo-001", "prop-paris-001", "prop-bali-001")) {
            final String c = candidate;
            if (g.getSavedHotels().stream().noneMatch(h -> h.hotelId().equals(c))) return c;
        }
        throw new IllegalStateException("All candidate hotels already saved");
    }

    @Test
    void save_hotel_is_idempotent_when_already_saved() {
        ds.saveHotel(existingGuestId, "prop-london-001");
        int afterFirst = ds.findById(existingGuestId).orElseThrow().getSavedHotels().size();
        ds.saveHotel(existingGuestId, "prop-london-001");
        assertThat(ds.findById(existingGuestId).orElseThrow().getSavedHotels())
                .hasSize(afterFirst);
    }

    @Test
    void save_hotel_unknown_guest_returns_null() {
        assertThat(ds.saveHotel("not-real", "prop-paris-001")).isNull();
    }

    @Test
    void unsave_hotel_removes_entry() {
        ds.saveHotel(existingGuestId, "prop-tokyo-001");
        ds.unsaveHotel(existingGuestId, "prop-tokyo-001");
        assertThat(ds.findById(existingGuestId).orElseThrow().getSavedHotels())
                .noneMatch(h -> h.hotelId().equals("prop-tokyo-001"));
    }

    @Test
    void unsave_hotel_unknown_guest_returns_null() {
        assertThat(ds.unsaveHotel("not-real", "prop-paris-001")).isNull();
    }

    // ── travel companions ────────────────────────────────────────────────────

    @Test
    void add_travel_companion_appends_with_full_name_record() {
        GuestProfile g = ds.findById(existingGuestId).orElseThrow();
        int before = g.getTravelCompanions().size();
        Map<String, Object> name = Map.of(
                "title", "Dr.", "firstName", "Jane", "middleName", "Q",
                "lastName", "Doe", "preferredName", "Janie");
        ds.addTravelCompanion(existingGuestId, Map.of(
                "name", name, "relationship", "SPOUSE",
                "email", "jane@example.com", "phone", "+1-555-0000",
                "loyaltyNumber", "LX99999", "dateOfBirth", "1985-06-15"));
        assertThat(g.getTravelCompanions()).hasSize(before + 1);
    }

    @Test
    void add_travel_companion_with_no_name_input_uses_default() {
        GuestProfile g = ds.findById(existingGuestId).orElseThrow();
        int before = g.getTravelCompanions().size();
        ds.addTravelCompanion(existingGuestId, Map.of("relationship", "FRIEND"));
        assertThat(g.getTravelCompanions()).hasSize(before + 1);
    }

    @Test
    void add_travel_companion_unknown_guest_returns_null() {
        assertThat(ds.addTravelCompanion("not-real", Map.of())).isNull();
    }

    @Test
    void remove_travel_companion_unknown_guest_returns_null() {
        assertThat(ds.removeTravelCompanion("not-real", "tc-1")).isNull();
    }

    @Test
    void remove_travel_companion_returns_profile_even_when_companion_not_found() {
        // the data source is permissive: returns the profile without throwing
        assertThat(ds.removeTravelCompanion(existingGuestId, "tc-not-real")).isNotNull();
    }

    // ── Address mutations ────────────────────────────────────────────────────

    @Test
    void add_address_appends_to_list_and_persists_fields() {
        int sizeBefore = ds.findById(existingGuestId).get().getAddresses().size();
        Map<String, Object> input = Map.of(
                "type", "BILLING",
                "line1", "500 Test Ave",
                "city", "Sacramento",
                "stateCode", "CA",
                "postalCode", "95814",
                "countryCode", "US"
        );
        GuestProfile updated = ds.addAddress(existingGuestId, input);
        assertThat(updated.getAddresses()).hasSize(sizeBefore + 1);
        var added = updated.getAddresses().get(updated.getAddresses().size() - 1);
        assertThat(added.id()).startsWith("addr-");
        assertThat(added.line1()).isEqualTo("500 Test Ave");
        assertThat(added.city()).isEqualTo("Sacramento");
        assertThat(added.stateCode()).isEqualTo("CA");
    }

    @Test
    void add_address_with_isPrimary_demotes_existing_primary() {
        Map<String, Object> input = Map.of(
                "type", "WORK",
                "line1", "1 New St",
                "city", "Oakland",
                "countryCode", "US",
                "isPrimary", true
        );
        GuestProfile updated = ds.addAddress(existingGuestId, input);
        long primaries = updated.getAddresses().stream().filter(a -> a.isPrimary()).count();
        assertThat(primaries).isEqualTo(1);
        var added = updated.getAddresses().get(updated.getAddresses().size() - 1);
        assertThat(added.isPrimary()).isTrue();
    }

    @Test
    void add_address_unknown_guest_returns_null() {
        assertThat(ds.addAddress("not-real", Map.of(
                "type", "HOME", "line1", "x", "city", "y", "countryCode", "US"
        ))).isNull();
    }

    @Test
    void update_address_only_changes_specified_fields() {
        var existing = ds.findById(existingGuestId).get().getAddresses().get(0);
        GuestProfile updated = ds.updateAddress(existingGuestId, existing.id(),
                Map.of("line1", "999 Modified Way"));
        var after = updated.getAddresses().stream()
                .filter(a -> a.id().equals(existing.id())).findFirst().orElseThrow();
        assertThat(after.line1()).isEqualTo("999 Modified Way");
        // Unchanged fields preserved.
        assertThat(after.city()).isEqualTo(existing.city());
        assertThat(after.countryCode()).isEqualTo(existing.countryCode());
        assertThat(after.isPrimary()).isEqualTo(existing.isPrimary());
    }

    @Test
    void update_address_promote_to_primary_demotes_old_primary() {
        var addrs = ds.findById(existingGuestId).get().getAddresses();
        // sophia has HOME primary + WORK non-primary; promote WORK
        var work = addrs.stream().filter(a -> !a.isPrimary()).findFirst().orElseThrow();
        GuestProfile updated = ds.updateAddress(existingGuestId, work.id(),
                Map.of("isPrimary", true));
        long primaries = updated.getAddresses().stream().filter(a -> a.isPrimary()).count();
        assertThat(primaries).isEqualTo(1);
        var promoted = updated.getAddresses().stream()
                .filter(a -> a.id().equals(work.id())).findFirst().orElseThrow();
        assertThat(promoted.isPrimary()).isTrue();
    }

    @Test
    void update_address_unknown_address_returns_null() {
        assertThat(ds.updateAddress(existingGuestId, "addr-nope", Map.of("city", "Nowhere"))).isNull();
    }

    @Test
    void remove_address_drops_entry() {
        var existing = ds.findById(existingGuestId).get().getAddresses().get(0);
        int before = ds.findById(existingGuestId).get().getAddresses().size();
        GuestProfile updated = ds.removeAddress(existingGuestId, existing.id());
        assertThat(updated).isNotNull();
        assertThat(updated.getAddresses()).hasSize(before - 1);
        assertThat(updated.getAddresses().stream().anyMatch(a -> a.id().equals(existing.id()))).isFalse();
    }

    @Test
    void remove_primary_address_promotes_first_remaining() {
        // sophia: HOME primary (index 0) + WORK non-primary (index 1)
        var primary = ds.findById(existingGuestId).get().getAddresses().get(0);
        assertThat(primary.isPrimary()).isTrue();
        GuestProfile updated = ds.removeAddress(existingGuestId, primary.id());
        long primaries = updated.getAddresses().stream().filter(a -> a.isPrimary()).count();
        assertThat(primaries).isEqualTo(1);
    }

    @Test
    void remove_address_unknown_returns_null() {
        assertThat(ds.removeAddress(existingGuestId, "addr-nope")).isNull();
    }

    @Test
    void set_primary_address_swaps_primary_flag() {
        var addrs = ds.findById(existingGuestId).get().getAddresses();
        var work = addrs.stream().filter(a -> !a.isPrimary()).findFirst().orElseThrow();
        GuestProfile updated = ds.setPrimaryAddress(existingGuestId, work.id());
        long primaries = updated.getAddresses().stream().filter(a -> a.isPrimary()).count();
        assertThat(primaries).isEqualTo(1);
        var nowPrimary = updated.getAddresses().stream()
                .filter(a -> a.isPrimary()).findFirst().orElseThrow();
        assertThat(nowPrimary.id()).isEqualTo(work.id());
    }

    @Test
    void set_primary_address_already_primary_is_noop() {
        var primary = ds.findById(existingGuestId).get().getAddresses().get(0);
        assertThat(primary.isPrimary()).isTrue();
        GuestProfile updated = ds.setPrimaryAddress(existingGuestId, primary.id());
        assertThat(updated).isNotNull();
        long primaries = updated.getAddresses().stream().filter(a -> a.isPrimary()).count();
        assertThat(primaries).isEqualTo(1);
    }

    @Test
    void set_primary_address_unknown_returns_null() {
        assertThat(ds.setPrimaryAddress(existingGuestId, "addr-nope")).isNull();
    }
}
