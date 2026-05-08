package com.luxe.pricing.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * USD-pivot foreign-exchange conversion — extracted out of
 * {@link PricingMockDataSource} so the FX table and the conversion
 * algorithm are a single-responsibility cross-cutting utility, not
 * pricing-specific logic. Same subgraph; just a clean internal seam
 * that makes future migration to a real FX provider (e.g. ECB feed,
 * Open Exchange Rates) straightforward.
 *
 * <p>Coverage spans every currency a hotel in the property subgraph
 * is denominated in (see {@code PropertyDataGenerator.COUNTRIES}).
 * A unit test pins the cardinality so adding a country without its
 * FX rate fails CI.
 */
public class FxConversionService {

    private static final Logger LOG = LoggerFactory.getLogger(FxConversionService.class);

    /**
     * Tracks currency pairs we've already warned about so the same gap
     * doesn't spam logs on every request. ConcurrentHashMap so it's
     * safe under DGS's thread pool. The set never grows beyond a handful
     * in practice — one entry per unsupported currency seen at runtime.
     */
    private static final Set<String> LOGGED_FX_GAPS = ConcurrentHashMap.newKeySet();

    /**
     * FX rates expressed as "1 unit of CCY = X USD" — the multiplier to
     * apply when converting from a foreign currency to USD. Approximate
     * market rates as of mid-2026; exact values aren't important for a
     * demo.
     */
    public static final Map<String, Double> RATES_TO_USD = Map.ofEntries(
            // North America
            Map.entry("USD",     1.0),
            Map.entry("CAD",     0.74),
            Map.entry("MXN",     0.058),
            // South America
            Map.entry("BRL",     0.20),
            Map.entry("ARS",     0.001),
            Map.entry("CLP",     0.0011),
            Map.entry("PEN",     0.27),
            // Western & Northern Europe
            Map.entry("EUR",     1.07),
            Map.entry("GBP",     1.27),
            Map.entry("CHF",     1.13),
            Map.entry("SEK",     0.094),
            Map.entry("NOK",     0.094),
            Map.entry("DKK",     0.144),
            Map.entry("ISK",     0.0072),
            // Eastern Europe
            Map.entry("PLN",     0.247),
            Map.entry("CZK",     0.043),
            Map.entry("HUF",     0.0028),
            // East Asia
            Map.entry("JPY",     0.0067),
            Map.entry("KRW",     0.00073),
            Map.entry("CNY",     0.14),
            Map.entry("HKD",     0.128),
            Map.entry("TWD",     0.031),
            // Southeast Asia
            Map.entry("SGD",     0.74),
            Map.entry("THB",     0.029),
            Map.entry("MYR",     0.214),
            Map.entry("IDR",     0.0000625),
            Map.entry("VND",     0.0000405),
            Map.entry("PHP",     0.0173),
            // South Asia
            Map.entry("INR",     0.012),
            Map.entry("LKR",     0.0034),
            // Middle East
            Map.entry("AED",     0.272),
            Map.entry("SAR",     0.267),
            Map.entry("QAR",     0.275),
            Map.entry("OMR",     2.60),
            Map.entry("ILS",     0.275),
            Map.entry("JOD",     1.41),
            // Africa
            Map.entry("EGP",     0.020),
            Map.entry("MAD",     0.10),
            Map.entry("ZAR",     0.054),
            Map.entry("KES",     0.0078),
            // Oceania
            Map.entry("AUD",     0.66),
            Map.entry("NZD",     0.61)
    );

    /**
     * Convert {@code amount} from one currency to another via USD as the
     * pivot. If either currency is unknown, returns the amount unchanged
     * so the page still renders rather than 500-ing — but the
     * label/amount may then be misaligned, which is preferable to a
     * broken response. Each missing pair is logged at WARN exactly once
     * so silent data drift surfaces in logs without spamming every
     * request.
     */
    public static double convert(double amount, String from, String to) {
        if (from == null || to == null || from.equals(to)) return amount;
        String fromUpper = from.toUpperCase();
        String toUpper   = to.toUpperCase();
        Double fromRate = RATES_TO_USD.get(fromUpper);
        Double toRate   = RATES_TO_USD.get(toUpper);
        if (fromRate == null || toRate == null) {
            String key = fromUpper + "->" + toUpper;
            if (LOGGED_FX_GAPS.add(key)) {
                LOG.warn("Pricing: missing FX rate for {} (from={} to={}); passing amount through unchanged",
                        key,
                        fromRate == null ? fromUpper : "ok",
                        toRate == null ? toUpper : "ok");
            }
            return amount;
        }
        double usd = amount * fromRate;
        return usd / toRate;
    }

    private FxConversionService() {
        // Utility class — pure functions and a static rate table; no
        // per-instance state to manage.
    }
}
