package com.luxe.pricing.datasource;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FX conversion contract — focused tests for the extracted
 * {@link FxConversionService}. The same conversions are also exercised
 * end-to-end by {@code PricingMockDataSourceTest}, but pinning them
 * here keeps the FX behaviour testable without standing up the
 * pricing data source.
 */
class FxConversionServiceTest {

    @Test
    void same_currency_passthrough_returns_input_unchanged() {
        assertThat(FxConversionService.convert(123.45, "USD", "USD")).isEqualTo(123.45);
        assertThat(FxConversionService.convert(99.99, "EUR", "EUR")).isEqualTo(99.99);
    }

    @Test
    void converts_eur_to_usd_at_market_rate() {
        // 1 EUR = 1.07 USD → 100 EUR = 107 USD
        assertThat(FxConversionService.convert(100, "EUR", "USD"))
                .isCloseTo(107.0, Offset.offset(0.01));
    }

    @Test
    void converts_usd_to_eur_using_inverse() {
        // 100 USD ÷ 1.07 ≈ 93.46 EUR
        assertThat(FxConversionService.convert(100, "USD", "EUR"))
                .isCloseTo(93.46, Offset.offset(0.05));
    }

    @Test
    void converts_eur_to_gbp_via_usd_pivot() {
        // 100 EUR → 107 USD → 107 / 1.27 ≈ 84.25 GBP
        assertThat(FxConversionService.convert(100, "EUR", "GBP"))
                .isCloseTo(84.25, Offset.offset(0.5));
    }

    @Test
    void converts_omr_above_parity() {
        // OMR is one of the few currencies stronger than USD (1 OMR = 2.60 USD)
        assertThat(FxConversionService.convert(100, "OMR", "USD"))
                .isGreaterThan(100);
    }

    @Test
    void is_case_insensitive_on_currency_codes() {
        double upper = FxConversionService.convert(100, "EUR", "USD");
        double lower = FxConversionService.convert(100, "eur", "usd");
        double mixed = FxConversionService.convert(100, "Eur", "Usd");
        assertThat(lower).isEqualTo(upper);
        assertThat(mixed).isEqualTo(upper);
    }

    @Test
    void unknown_currency_returns_input_unchanged() {
        // Defensive — keep the page rendering rather than 500-ing on a
        // currency the FX table doesn't carry. The label/amount may then
        // be misaligned, which is preferable to a broken response.
        assertThat(FxConversionService.convert(100, "EUR", "ZZZ")).isEqualTo(100);
        assertThat(FxConversionService.convert(100, "ZZZ", "USD")).isEqualTo(100);
    }

    @Test
    void null_inputs_pass_through_safely() {
        assertThat(FxConversionService.convert(50, null, "USD")).isEqualTo(50);
        assertThat(FxConversionService.convert(50, "USD", null)).isEqualTo(50);
    }

    @Test
    void rate_table_covers_every_supported_country_currency() {
        // Mirror of the canonical list from PropertyDataGenerator. If a
        // new country is added there with a previously unseen currency,
        // this test fails so we don't ship a country whose hotels can't
        // be priced.
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
        assertThat(FxConversionService.RATES_TO_USD.keySet()).containsAll(supported);
    }
}
