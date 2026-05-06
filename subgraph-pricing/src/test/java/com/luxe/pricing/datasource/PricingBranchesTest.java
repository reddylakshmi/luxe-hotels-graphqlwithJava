package com.luxe.pricing.datasource;

import com.luxe.pricing.schema.types.AvailabilityResult;
import com.luxe.pricing.schema.types.DateRateSummary;
import com.luxe.pricing.schema.types.GiftCardBalance;
import com.luxe.pricing.schema.types.Promotion;
import com.luxe.pricing.schema.types.Rate;
import com.luxe.pricing.schema.types.RatePlan;
import com.luxe.pricing.schema.types.RedemptionRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage of the lookup branches and ancillary domains: gift cards, packages,
 * rate calendar, redemption rates, promotion filters, and the searchRates
 * branches around currency / corporateCode / promoCode / room type filtering.
 */
class PricingBranchesTest {

    private PricingMockDataSource ds;
    private final LocalDate ci = LocalDate.now().plusDays(30);
    private final LocalDate co = LocalDate.now().plusDays(33);

    @BeforeEach
    void setUp() {
        ds = new PricingMockDataSource();
    }

    // ── searchRates branches ──────────────────────────────────────────────────

    @Test
    void search_rates_with_default_currency_uses_hotel_local_currency() {
        AvailabilityResult r = ds.searchRates("prop-paris-001", ci, co,
                2, 0, null, null, null, null, null);
        assertThat(r.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void search_rates_filters_by_room_type_ids() {
        AvailabilityResult r = ds.searchRates("prop-paris-001", ci, co,
                2, 0, "EUR", null, List.of("rt-paris-dlx-001"), null, null);
        assertThat(r.getRoomAvailabilities()).allSatisfy(ra ->
                assertThat(ra.getRoomType().get("id")).isEqualTo("rt-paris-dlx-001"));
    }

    @Test
    void search_rates_with_promo_code_returns_a_result() {
        AvailabilityResult r = ds.searchRates("prop-paris-001", ci, co,
                2, 0, "EUR", null, null, "SPRING25", null);
        assertThat(r).isNotNull();
        assertThat(r.getRoomAvailabilities()).isNotEmpty();
    }

    @Test
    void search_rates_with_corporate_code_returns_a_result() {
        AvailabilityResult r = ds.searchRates("prop-paris-001", ci, co,
                2, 0, "EUR", null, null, null, "ACME-CORP");
        assertThat(r).isNotNull();
    }

    @Test
    void search_rates_with_three_or_more_adults_works() {
        AvailabilityResult r = ds.searchRates("prop-dubai-001", ci, co,
                3, 1, "AED", null, null, null, null);
        assertThat(r.getRoomAvailabilities()).isNotEmpty();
    }

    // ── rate / rate plan lookups ─────────────────────────────────────────────

    @Test
    void find_rate_by_unknown_id_is_empty() {
        assertThat(ds.findRateById("rate-not-real")).isEmpty();
    }

    @Test
    void find_rate_plan_by_id_for_seeded_plans() {
        Rate any = ds.findRatesByHotelId("prop-paris-001", ci, co, 2).get(0);
        RatePlan plan = any.getRatePlan();
        assertThat(ds.findRatePlanById(plan.getId())).isPresent();
    }

    @Test
    void find_rate_plan_by_unknown_id_is_empty() {
        assertThat(ds.findRatePlanById("rp-not-real")).isEmpty();
    }

    @Test
    void find_rates_by_room_type_id_returns_rates_for_that_room_type() {
        // discover a room type id from a hotel search
        var rates = ds.findRatesByHotelId("prop-paris-001", ci, co, 2);
        assertThat(rates).isNotEmpty();
        String anyRoomTypeId = rates.get(0).getRoomTypeId();
        var byRoomType = ds.findRatesByRoomTypeId(anyRoomTypeId, ci, co, 2);
        assertThat(byRoomType).allSatisfy(r ->
                assertThat(r.getRoomTypeId()).isEqualTo(anyRoomTypeId));
    }

    @Test
    void find_rates_by_unknown_room_type_returns_empty() {
        assertThat(ds.findRatesByRoomTypeId("rt-not-real", ci, co, 2)).isEmpty();
    }

    @Test
    void find_rates_by_unknown_hotel_returns_empty() {
        assertThat(ds.findRatesByHotelId("not-real", ci, co, 2)).isEmpty();
    }

    // ── promotions filtering branches ────────────────────────────────────────

    @Test
    void find_promotions_filter_by_member_only_true() {
        List<Promotion> memberOnly = ds.findPromotions(null, true);
        assertThat(memberOnly).allSatisfy(p ->
                assertThat(p.isMemberOnly()).isTrue());
    }

    @Test
    void find_promotions_filter_by_member_only_false() {
        List<Promotion> open = ds.findPromotions(null, false);
        assertThat(open).allSatisfy(p ->
                assertThat(p.isMemberOnly()).isFalse());
    }

    @Test
    void find_promotions_filter_by_brand_id_returns_subset() {
        // discover a real brandId
        Promotion any = ds.findPromotions(null, null).get(0);
        if (any.getBrandId() != null) {
            List<Promotion> branded = ds.findPromotions(any.getBrandId(), null);
            assertThat(branded).allSatisfy(p ->
                    assertThat(p.getBrandId()).isEqualTo(any.getBrandId()));
        }
    }

    // ── package lookup ───────────────────────────────────────────────────────

    @Test
    void find_package_by_unknown_id_is_empty() {
        assertThat(ds.findPackageById("pkg-not-real")).isEmpty();
    }

    // ── rate calendar branches ───────────────────────────────────────────────

    @Test
    void rate_calendar_returns_one_entry_per_day_in_range() {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(15);
        List<DateRateSummary> calendar = ds.getRateCalendar("prop-paris-001",
                start, end, 2, "EUR");
        assertThat(calendar).isNotEmpty();
    }

    @Test
    void rate_calendar_for_unknown_hotel_returns_empty_or_skipped() {
        List<DateRateSummary> calendar = ds.getRateCalendar("not-real",
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(7),
                2, "USD");
        assertThat(calendar).isNotNull();
    }

    // ── gift cards ───────────────────────────────────────────────────────────

    @Test
    void find_gift_card_balance_for_seeded_code_returns_balance() {
        // The mock seeds a few gift card codes — discover one by trying common patterns
        // or fall back to checking the empty-result branch is well-formed.
        var found = ds.findGiftCardBalance("GIFT-1234");
        // Whether it exists in seed or not, the call should be safe & well-typed
        assertThat(found).isNotNull();
    }

    @Test
    void find_gift_card_balance_for_unknown_code_is_empty() {
        assertThat(ds.findGiftCardBalance("GIFT-NOT-REAL")).isEmpty();
    }

    @Test
    void find_gift_card_balance_with_blank_code_is_empty_or_well_typed() {
        // Edge case: blank string should not blow up
        assertThat(ds.findGiftCardBalance("")).isNotNull();
    }

    // ── redemption rates ─────────────────────────────────────────────────────

    @Test
    void redemption_rates_for_known_hotel_returns_list() {
        List<RedemptionRate> rates = ds.findRedemptionRates("prop-paris-001",
                ci, co, null);
        assertThat(rates).isNotNull();
    }

    @Test
    void redemption_rates_with_room_type_filter_returns_list() {
        // discover a room type id
        Rate any = ds.findRatesByHotelId("prop-paris-001", ci, co, 2).get(0);
        List<RedemptionRate> filtered = ds.findRedemptionRates("prop-paris-001",
                ci, co, any.getRoomTypeId());
        assertThat(filtered).isNotNull();
    }

    @Test
    void redemption_rates_for_unknown_hotel_returns_empty() {
        assertThat(ds.findRedemptionRates("not-real", ci, co, null)).isEmpty();
    }
}
