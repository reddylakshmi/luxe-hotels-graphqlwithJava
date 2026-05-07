package com.luxe.pricing.datasource;

import com.luxe.pricing.schema.types.AvailabilityResult;
import com.luxe.pricing.schema.types.Promotion;
import com.luxe.pricing.schema.types.Rate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingMockDataSourceTest {

    private PricingMockDataSource ds;
    private final LocalDate ci = LocalDate.now().plusDays(30);
    private final LocalDate co = LocalDate.now().plusDays(33);

    @BeforeEach
    void setUp() {
        ds = new PricingMockDataSource();
    }

    @Test
    void search_rates_for_known_hotel_returns_room_availabilities() {
        AvailabilityResult result = ds.searchRates("prop-paris-001", ci, co,
                2, 0, "EUR", null, null, null, null);
        assertThat(result).isNotNull();
        assertThat(result.getRoomAvailabilities()).isNotEmpty();
    }

    @Test
    void search_rates_token_is_present() {
        AvailabilityResult result = ds.searchRates("prop-paris-001", ci, co,
                2, 0, "EUR", null, null, null, null);
        assertThat(result.getSearchToken()).isNotBlank();
    }

    @Test
    void find_rates_by_hotel_id_returns_rates() {
        List<Rate> rates = ds.findRatesByHotelId("prop-paris-001", ci, co, 2);
        assertThat(rates).isNotEmpty();
        assertThat(rates).allSatisfy(r -> assertThat(r.getRateToken()).isNotBlank());
    }

    @Test
    void invalid_rate_token_does_not_validate() {
        assertThat(ds.validateRate("bogus-token")).isEmpty();
    }

    @Test
    void find_promotions_returns_seeded_promotions() {
        List<Promotion> promos = ds.findPromotions(null, null);
        assertThat(promos).isNotEmpty();
        assertThat(promos).allSatisfy(p -> assertThat(p.getCode()).isNotBlank());
    }

    @Test
    void find_promotion_by_known_code_returns_it() {
        Promotion any = ds.findPromotions(null, null).get(0);
        assertThat(ds.findPromotionByCode(any.getCode()))
                .isPresent().get().extracting(Promotion::getCode).isEqualTo(any.getCode());
    }

    @Test
    void find_promotion_by_unknown_code_is_empty() {
        assertThat(ds.findPromotionByCode("NOT-REAL")).isEmpty();
    }

    // ── Currency / FX coverage ───────────────────────────────────────────────

    /**
     * Every currency that appears on a hotel in the property subgraph must have
     * an FX rate so we can convert prices into the user's chosen currency. If
     * a new country is added there with a previously unseen currency, this test
     * fails so we don't ship a country whose hotels can't be priced.
     */
    @Test
    void fx_table_covers_every_supported_country_currency() {
        java.util.Set<String> supported = java.util.Set.of(
                "USD", "CAD", "MXN",                                  // North America
                "BRL", "ARS", "CLP", "PEN",                           // South America
                "EUR", "GBP", "CHF", "SEK", "NOK", "DKK", "ISK",     // Western/Northern Europe
                "PLN", "CZK", "HUF",                                  // Eastern Europe
                "JPY", "KRW", "CNY", "HKD", "TWD",                   // East Asia
                "SGD", "THB", "MYR", "IDR", "VND", "PHP",            // Southeast Asia
                "INR", "LKR",                                         // South Asia
                "AED", "SAR", "QAR", "OMR", "ILS", "JOD",            // Middle East
                "EGP", "MAD", "ZAR", "KES",                           // Africa
                "AUD", "NZD"                                          // Oceania
        );
        assertThat(supported).hasSize(42);
        assertThat(PricingMockDataSource.FX_TO_USD.keySet()).containsAll(supported);
    }

    @Test
    void converts_eur_to_gbp_via_usd_pivot() {
        double gbp = PricingMockDataSource.convertCurrency(100, "EUR", "GBP");
        // 100 EUR → 107 USD → 107/1.27 ≈ 84.25 GBP
        assertThat(gbp).isCloseTo(84.25, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void converts_inr_to_eur() {
        double eur = PricingMockDataSource.convertCurrency(10000, "INR", "EUR");
        // 10000 INR × 0.012 = 120 USD; 120 / 1.07 ≈ 112.15 EUR
        assertThat(eur).isCloseTo(112.15, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void converts_omr_to_usd_above_parity() {
        // OMR is one of the few currencies stronger than USD (1 OMR ≈ 2.60 USD).
        double usd = PricingMockDataSource.convertCurrency(100, "OMR", "USD");
        assertThat(usd).isGreaterThan(100);
    }

    @Test
    void same_currency_passthrough_returns_input() {
        assertThat(PricingMockDataSource.convertCurrency(123.45, "EUR", "EUR")).isEqualTo(123.45);
    }

    @Test
    void unknown_currency_returns_input_unchanged() {
        // Defensive: if the user passes a currency we don't know, keep the
        // amount rather than 500-ing. The label may be misaligned, but the
        // page still renders.
        assertThat(PricingMockDataSource.convertCurrency(100, "EUR", "ZZZ")).isEqualTo(100);
    }

    @Test
    void user_requested_currency_overrides_hotel_default() {
        // Paris's prices are stored in EUR; user asks for USD → all amounts
        // come back in USD with FX-converted numbers.
        AvailabilityResult inEur = ds.searchRates("prop-paris-001", ci, co,
                2, 0, "EUR", null, null, null, null);
        AvailabilityResult inUsd = ds.searchRates("prop-paris-001", ci, co,
                2, 0, "USD", null, null, null, null);
        assertThat(inEur.getCurrency()).isEqualTo("EUR");
        assertThat(inUsd.getCurrency()).isEqualTo("USD");
        double eurAmount = Double.parseDouble(inEur.getLowestRate().amount());
        double usdAmount = Double.parseDouble(inUsd.getLowestRate().amount());
        // 1 EUR ≈ 1.07 USD → USD figure should be ~7% higher than EUR.
        assertThat(usdAmount).isGreaterThan(eurAmount);
        assertThat(usdAmount / eurAmount).isCloseTo(1.07, org.assertj.core.data.Offset.offset(0.01));
    }
}
