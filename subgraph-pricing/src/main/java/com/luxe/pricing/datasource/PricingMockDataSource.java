package com.luxe.pricing.datasource;

import com.luxe.common.scalar.Money;
import com.luxe.pricing.schema.types.*;
import com.luxe.pricing.schema.types.Package;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PricingMockDataSource implements PricingDataSource {

    private final Map<String, RatePlan> ratePlans = new LinkedHashMap<>();
    private final Map<String, Promotion> promotions = new LinkedHashMap<>();
    private final Map<String, Package> packages = new LinkedHashMap<>();
    private final Map<String, GiftCardBalance> giftCards = new LinkedHashMap<>();

    // hotelId -> roomTypeId -> [basePriceFlexible, basePriceNonRefundable, currencyMul, pointsPerNight]
    // The room IDs here match the actual IDs used by the property subgraph
    // (PropertyMockDataSource hand-curated hotels). Anything not in this map
    // is served by the deterministic fallback generator below.
    private static final Map<String, Map<String, double[]>> HOTEL_ROOMS = new LinkedHashMap<>();

    static {
        HOTEL_ROOMS.put("prop-paris-001", new LinkedHashMap<>(Map.of(
                "rt-paris-deluxe", new double[]{520, 415, 1.0, 1500},
                "rt-paris-suite",  new double[]{720, 576, 1.0, 2500}
        )));
        HOTEL_ROOMS.put("prop-tokyo-001", new LinkedHashMap<>(Map.of(
                "rt-tokyo-deluxe", new double[]{75000, 60000, 1.0, 1200},
                "rt-tokyo-suite",  new double[]{180000, 144000, 1.0, 3000}
        )));
        HOTEL_ROOMS.put("prop-dubai-001", new LinkedHashMap<>(Map.of(
                "rt-dubai-deluxe", new double[]{1100, 880, 1.0, 1500}
        )));
        HOTEL_ROOMS.put("prop-nyc-001", new LinkedHashMap<>(Map.of(
                "rt-nyc-deluxe", new double[]{720, 576, 1.0, 1800}
        )));
        HOTEL_ROOMS.put("prop-london-001", new LinkedHashMap<>(Map.of(
                "rt-london-deluxe", new double[]{580, 464, 1.0, 1600}
        )));
    }

    private static final Map<String, String> HOTEL_CURRENCY = Map.of(
            "prop-paris-001", "EUR", "prop-tokyo-001", "JPY",
            "prop-dubai-001", "AED", "prop-nyc-001", "USD",
            "prop-london-001", "GBP"
    );

    private static final Map<String, Double> HOTEL_TAX_RATE = Map.of(
            "prop-paris-001", 0.10, "prop-tokyo-001", 0.10,
            "prop-dubai-001", 0.10, "prop-nyc-001", 0.15,
            "prop-london-001", 0.20
    );

    /**
     * FX rates expressed as "1 unit of CCY = X USD" — the multiplier to apply
     * when converting from a foreign currency to USD. Approximate market rates
     * as of mid-2026; exact values aren't important for a demo. Coverage spans
     * every currency a hotel in the property subgraph is denominated in (see
     * {@code PropertyDataGenerator.COUNTRIES}). A unit test in pricing pins
     * the cardinality so adding a country without its FX rate fails CI.
     */
    static final Map<String, Double> FX_TO_USD = Map.ofEntries(
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
     * Convert {@code amount} from one currency to another via USD as the pivot.
     * If either currency is unknown, returns the amount unchanged so the page
     * still renders rather than 500-ing — but the label/amount may then be
     * misaligned, which is preferable to a broken response.
     */
    static double convertCurrency(double amount, String from, String to) {
        if (from == null || to == null || from.equals(to)) return amount;
        Double fromRate = FX_TO_USD.get(from.toUpperCase());
        Double toRate = FX_TO_USD.get(to.toUpperCase());
        if (fromRate == null || toRate == null) return amount;
        double usd = amount * fromRate;
        return usd / toRate;
    }

    /** India IT-corridor hotels seeded as 5★ in the property subgraph — these
     * also have a `-rm-ste` room. The other 12 India hotels stop at -rm-exe. */
    private static final Set<String> INDIA_FIVE_STAR_HOTELS = Set.of(
            "prop-india-bom-bkc",
            "prop-india-del-cyber",
            "prop-india-hyd-hitec",
            "prop-india-blr-white",
            "prop-india-maa-omr"
    );

    private static final CancellationPolicy FREE_48H = new CancellationPolicy(
            "FREE_CANCELLATION", "Free cancellation up to 48 hours before check-in", 48);
    private static final CancellationPolicy FREE_72H = new CancellationPolicy(
            "FREE_CANCELLATION", "Free cancellation up to 72 hours before check-in", 72);
    private static final CancellationPolicy NON_REF = new CancellationPolicy(
            "NON_REFUNDABLE", "Non-refundable — no changes or cancellations", null);

    public PricingMockDataSource() {
        initRatePlans();
        initPromotions();
        initPackages();
        initGiftCards();
    }

    private void initRatePlans() {
        add(new RatePlan("rp-bar", "BAR", "Flexible Rate", "BEST_AVAILABLE",
                "Our best flexible rate with free cancellation. Taxes and all fees included.",
                true, false, true, false,
                true, 1.0, FREE_48H, 1, null, null));
        add(new RatePlan("rp-member", "MEMBER", "Member Exclusive Offer", "MEMBER_RATE",
                "Includes 3000 Bonus Points per night, based upon availability. Fully refundable.",
                true, false, true, false,
                true, 1.5, FREE_48H, 1, null, 0));
        add(new RatePlan("rp-pkg-stay", "PKG-STAY", "Package Rate", "PACKAGE",
                "Room with daily breakfast and complimentary parking included.",
                true, true, true, true,
                true, 1.2, FREE_72H, 1, null, null));
        // Other plans kept for promotion/corporate/redemption use-cases.
        add(new RatePlan("rp-nr", "NR", "Non-Refundable Saver", "BEST_AVAILABLE",
                "Lowest price — non-refundable, no changes", false, false, true, false,
                true, 1.0, NON_REF, 1, null, null));
        add(new RatePlan("rp-advance30", "ADV30", "Advance Purchase 30", "ADVANCE_PURCHASE",
                "Save 20% when booking 30+ days in advance", false, false, true, false,
                true, 1.0, NON_REF, 1, null, 30));
        add(new RatePlan("rp-corp", "CORP", "Corporate Rate", "CORPORATE",
                "Negotiated rate for corporate accounts", true, false, true, true,
                true, 1.0, FREE_48H, 1, null, 1));
        add(new RatePlan("rp-redemption", "REDEEM", "Points Redemption", "REDEMPTION",
                "Redeem loyalty points for complimentary nights", true, false, true, false,
                false, null, FREE_72H, 1, 7, null));
    }

    private void add(RatePlan rp) { ratePlans.put(rp.getId(), rp); }

    private void initPromotions() {
        LocalDate now = LocalDate.now();
        addPromo("promo-summer25", "SUMMER25", "Summer Escape 25% Off",
                "Enjoy 25% off your summer stay", "PERCENTAGE", 25.0, 3,
                "brand-lux-001", List.of("prop-paris-001", "prop-london-001"),
                false, false, 1, now.minusDays(10), now.plusDays(60));
        addPromo("promo-earlybird", "EARLYBIRD20", "Early Bird 20% Off",
                "Book 30+ days ahead and save 20%", "PERCENTAGE", 20.0, 2,
                null, List.of("prop-tokyo-001", "prop-dubai-001"),
                false, false, null, now.minusDays(5), now.plusDays(90));
        addPromo("promo-member15", "MEMBER15", "Member Exclusive 15% Off",
                "Exclusive 15% off for Luxe loyalty members", "PERCENTAGE", 15.0, 1,
                null, List.of(), true, false, null, now.minusDays(30), now.plusDays(180));
        addPromo("promo-weekend", "WEEKEND10", "Weekend Escape",
                "10% off weekend stays (Fri check-in)", "PERCENTAGE", 10.0, 2,
                "brand-prm-001", List.of("prop-london-001", "prop-nyc-001"),
                false, true, 2, now.minusDays(20), now.plusDays(120));
        addPromo("promo-flash", "FLASH30", "Flash Sale — 30% Off",
                "Limited time flash sale on select rooms", "PERCENTAGE", 30.0, 1,
                null, List.of("prop-nyc-001"),
                false, false, 1, now, now.plusDays(14));
    }

    private void addPromo(String id, String code, String name, String desc,
                           String discType, double discVal, Integer minStay,
                           String brandId, List<String> hotelIds,
                           boolean memberOnly, boolean stackable, Integer maxUses,
                           LocalDate from, LocalDate to) {
        promotions.put(code, new Promotion(id, code, name, desc, discType, discVal, minStay,
                brandId, hotelIds, memberOnly, stackable, maxUses, from, to, true));
    }

    private void initPackages() {
        LocalDate now = LocalDate.now();
        packages.put("pkg-paris-romance", new Package("pkg-paris-romance", "PARIS-ROMANCE",
                "Parisian Romance Package", "Champagne, flowers, and breakfast for two",
                "prop-paris-001", List.of("Champagne on arrival", "Daily breakfast",
                "Turndown service", "Late checkout (2pm)"),
                1.25, now, now.plusYears(1), true));
        packages.put("pkg-dubai-ultimate", new Package("pkg-dubai-ultimate", "DUBAI-ULTIMATE",
                "Ultimate Dubai Experience", "Helicopter tour + spa + butler service",
                "prop-dubai-001", List.of("Helicopter city tour", "Daily spa access",
                "Personal butler", "Daily breakfast and dinner"),
                1.60, now, now.plusYears(1), true));
        packages.put("pkg-tokyo-cultural", new Package("pkg-tokyo-cultural", "TOKYO-CULTURE",
                "Tokyo Cultural Immersion", "Tea ceremony, city tour, and sake welcome gift",
                "prop-tokyo-001", List.of("Tea ceremony experience", "Private city tour",
                "Sake welcome gift", "Daily breakfast"),
                1.30, now, now.plusYears(1), true));
    }

    private void initGiftCards() {
        giftCards.put("GC-LUXE-001", new GiftCardBalance("GC-LUXE-001",
                Money.of(500, "USD"), "USD",
                OffsetDateTime.now().plusYears(2), true));
        giftCards.put("GC-LUXE-002", new GiftCardBalance("GC-LUXE-002",
                Money.of(1000, "EUR"), "EUR",
                OffsetDateTime.now().plusYears(1), true));
    }

    // ── Rate Building ─────────────────────────────────────────────────────────

    private Rate buildRate(String rateId, String hotelId, String roomTypeId,
                            RatePlan plan, double baseNightly, String currency,
                            double taxRate, LocalDate checkIn, LocalDate checkOut,
                            int adults, Money strikethrough) {
        int nights = (int) checkIn.until(checkOut).getDays();
        double total = baseNightly * nights;
        double taxes = total * taxRate;
        double fees  = total * 0.02;

        List<NightlyRate> nightlyRates = checkIn.datesUntil(checkOut)
                .map(d -> new NightlyRate(d, Money.of(baseNightly, currency)))
                .collect(Collectors.toList());

        Money totalMoney  = Money.of(total, currency);
        Money withTaxes   = Money.of(total + taxes + fees, currency);
        Money avgNightly  = Money.of(baseNightly, currency);

        TaxBreakdown breakdown = new TaxBreakdown(
                Money.of(total, currency),
                Money.of(taxes, currency),
                Money.of(fees, currency),
                Money.of(total + taxes + fees, currency),
                List.of(
                        new TaxBreakdown.TaxLineItem("Value Added Tax",
                                Money.of(taxes, currency), "TAX"),
                        new TaxBreakdown.TaxLineItem("Resort Fee",
                                Money.of(fees, currency), "FEE")
                ));

        Money savings = strikethrough != null
                ? Money.of(Double.parseDouble(strikethrough.amount()) - baseNightly, currency) : null;
        Double savingsPct = strikethrough != null
                ? (1 - baseNightly / Double.parseDouble(strikethrough.amount())) * 100 : null;

        int pointsEarned = plan.isLoyaltyEligible()
                ? (int) (total * (plan.getLoyaltyMultiplier() != null ? plan.getLoyaltyMultiplier() : 1.0))
                : 0;

        int availableRooms = 1 + (int) (Math.abs(rateId.hashCode()) % 5);
        String rateToken = "rt-" + rateId + "-" + checkIn + "-" + checkOut;

        return new Rate(rateId, hotelId, roomTypeId, plan, nightlyRates,
                totalMoney, withTaxes, avgNightly, breakdown,
                strikethrough, savings, savingsPct,
                pointsEarned, availableRooms, rateToken,
                OffsetDateTime.now().plusMinutes(30));
    }

    // ── Room and tier inference ───────────────────────────────────────────────

    /**
     * Get the rooms map for a hotel. Hand-curated entries win; everything else
     * uses a deterministic fallback based on the established room-ID convention
     * in the property subgraph.
     */
    private Map<String, double[]> roomsForHotel(String hotelId) {
        Map<String, double[]> handCurated = HOTEL_ROOMS.get(hotelId);
        if (handCurated != null) return handCurated;
        return generateFallbackRooms(hotelId);
    }

    /**
     * Generate a synthetic room+price map for a hotel we don't have hand-curated
     * data for. Uses two ID conventions known to match the property subgraph:
     *
     * <ul>
     *   <li>India IT-corridor hotels — {@code {hotelId}-rm-dlx}, {@code -rm-exe},
     *       optionally {@code -rm-ste} for 5★ properties.</li>
     *   <li>Generator hotels (everything else under {@code prop-}) —
     *       {@code {hotelId}-rm1}, {@code -rm2}, {@code -rm3}.</li>
     * </ul>
     */
    private Map<String, double[]> generateFallbackRooms(String hotelId) {
        Map<String, double[]> rooms = new LinkedHashMap<>();
        String tier = inferTier(hotelId);
        if (hotelId.startsWith("prop-india-")) {
            rooms.put(hotelId + "-rm-dlx", priceTuple(hotelId, "dlx", tier));
            rooms.put(hotelId + "-rm-exe", priceTuple(hotelId, "exe", tier));
            if (INDIA_FIVE_STAR_HOTELS.contains(hotelId)) {
                rooms.put(hotelId + "-rm-ste", priceTuple(hotelId, "ste", tier));
            }
        } else if (hotelId.startsWith("prop-")) {
            rooms.put(hotelId + "-rm1", priceTuple(hotelId, "rm1", tier));
            rooms.put(hotelId + "-rm2", priceTuple(hotelId, "rm2", tier));
            rooms.put(hotelId + "-rm3", priceTuple(hotelId, "rm3", tier));
        }
        return rooms;
    }

    /** Build the {flex, nonRefundable, currencyMul, pointsPerNight} tuple. */
    private static double[] priceTuple(String hotelId, String roomKey, String tier) {
        int seed = Math.abs((hotelId + ":" + roomKey).hashCode());
        double base = switch (tier) {
            case "LUXURY"  -> 350 + (seed % 800);
            case "PREMIUM" -> 180 + (seed % 220);
            case "SELECT"  ->  95 + (seed % 130);
            default        -> 200;
        };
        // Bigger rooms cost more.
        double sizeMul = switch (roomKey) {
            case "rm1", "dlx" -> 1.0;
            case "rm2", "exe" -> 1.3;
            case "rm3", "ste" -> 1.8;
            default           -> 1.0;
        };
        base *= sizeMul;
        double flex = Math.round(base);
        double nonRef = Math.round(flex * 0.82);
        double pointsPerNight = Math.round(flex * 4);
        return new double[]{flex, nonRef, 1.0, pointsPerNight};
    }

    /**
     * Infer a hotel's brand tier without going to another subgraph. Hand-crafted
     * IDs are listed; generator IDs encode the brand code in the prefix
     * (e.g. {@code prop-mai-fr-paris} → MAI → LUXURY).
     */
    private static String inferTier(String hotelId) {
        if (LUXURY_HOTEL_IDS.contains(hotelId)) return "LUXURY";
        if (PREMIUM_HOTEL_IDS.contains(hotelId)) return "PREMIUM";
        // Generator pattern: prop-{brandCode}-{country}-{city}.
        if (hotelId.matches("^prop-(mai|atl|aur|rgl)-.*")) return "LUXURY";
        if (hotelId.matches("^prop-(mqs|lum|qlb|way|crd|hrh|wst)-.*")) return "SELECT";
        return "PREMIUM";
    }

    /** Hotels that should price at LUXURY tier — the 5 hand-curated luxes plus
     *  the two India MAI flagships. */
    private static final Set<String> LUXURY_HOTEL_IDS = Set.of(
            "prop-paris-001", "prop-tokyo-001", "prop-dubai-001", "prop-london-001",
            "prop-india-bom-bkc", "prop-india-del-cyber"
    );

    /** Hotels that should price at PREMIUM tier — currently only NYC. India
     *  business hotels and generator-PREMIUM brands fall through to PREMIUM via
     *  the inferTier default path. */
    private static final Set<String> PREMIUM_HOTEL_IDS = Set.of("prop-nyc-001");

    // ── PricingDataSource ─────────────────────────────────────────────────────

    @Override
    public AvailabilityResult searchRates(String hotelId, LocalDate checkIn, LocalDate checkOut,
                                           int adults, int children, String currency,
                                           List<String> ratePlanCodes, List<String> roomTypeIds,
                                           String promoCode, String corporateCode) {
        Map<String, double[]> rooms = roomsForHotel(hotelId);
        // Two currencies in play:
        //   • baseCurrency — the currency the price tuples are stored in (EUR
        //     for Paris, JPY for Tokyo, USD for India + generator hotels).
        //   • displayCurrency — what the user asked for. We honour the
        //     request and convert all amounts via FX.
        String baseCurrency = HOTEL_CURRENCY.getOrDefault(hotelId, "USD");
        String displayCurrency = (currency != null && !currency.isBlank())
                ? currency.toUpperCase() : baseCurrency;
        double fx = convertCurrency(1.0, baseCurrency, displayCurrency);
        double taxRate = HOTEL_TAX_RATE.getOrDefault(hotelId, 0.10);

        Promotion promoDiscount = promoCode != null
                ? promotions.get(promoCode.toUpperCase()) : null;
        double discount = (promoDiscount != null && promoDiscount.isActive())
                ? promoDiscount.getDiscountValue() / 100.0 : 0.0;

        boolean isCorporate = corporateCode != null && !corporateCode.isBlank();

        List<RoomAvailability> roomAvails = rooms.entrySet().stream()
                .filter(e -> roomTypeIds == null || roomTypeIds.isEmpty() || roomTypeIds.contains(e.getKey()))
                .map(e -> {
                    String rtId = e.getKey();
                    double[] prices = e.getValue();
                    // Convert base→display once per room so all derived numbers
                    // (per-rate-plan multipliers, totals, taxes) stay in sync.
                    double flexBase = prices[0] * (1 - discount) * fx;
                    double nrBase   = prices[1] * (1 - discount) * fx;

                    // Default trio shown on the rate-list page: Flexible (most popular),
                    // Member Exclusive Offer (with bonus points), Package Rate.
                    List<RatePlan> plans = new ArrayList<>(List.of(
                            ratePlans.get("rp-bar"),
                            ratePlans.get("rp-member"),
                            ratePlans.get("rp-pkg-stay")));
                    if (isCorporate) plans.add(ratePlans.get("rp-corp"));

                    if (ratePlanCodes != null && !ratePlanCodes.isEmpty()) {
                        plans = ratePlans.values().stream()
                                .filter(p -> ratePlanCodes.contains(p.getCode()))
                                .collect(Collectors.toList());
                    }

                    List<Rate> rates = new ArrayList<>();
                    for (RatePlan plan : plans) {
                        if (plan == null) continue;
                        double base = plan.isRefundable() ? flexBase : nrBase;
                        // Package adds breakfast + parking value (~5% bump);
                        // Member exclusive priced higher because it includes guaranteed
                        // bonus points (~3000/night) on top of the base rate.
                        switch (plan.getCode()) {
                            case "PKG-STAY" -> base *= 1.05;
                            case "MEMBER"   -> base *= 1.235;
                        }
                        Money strike = plan.isRefundable() ? null : Money.of(flexBase, displayCurrency);
                        String rateId = "r-" + rtId + "-" + plan.getCode().toLowerCase() + "-"
                                + checkIn + "-" + checkOut;
                        rates.add(buildRate(rateId, hotelId, rtId, plan, base,
                                displayCurrency, taxRate, checkIn, checkOut, adults, strike));
                    }

                    Rate lowest = rates.stream()
                            .min(Comparator.comparingDouble(r -> Double.parseDouble(r.getTotalRate().amount())))
                            .orElse(null);

                    int count = 1 + (int) (Math.abs(rtId.hashCode()) % 5);
                    UrgencySignal urgency = count <= 2
                            ? new UrgencySignal("LOW_AVAILABILITY",
                                    "Only " + count + " room" + (count == 1 ? "" : "s") + " left!", count)
                            : null;

                    return new RoomAvailability(rtId, rates, lowest, count, urgency);
                })
                .collect(Collectors.toList());

        Money globalLowest = roomAvails.stream()
                .filter(ra -> ra.getLowestRate() != null)
                .map(ra -> ra.getLowestRate().getTotalRate())
                .min(Comparator.comparingDouble(m -> Double.parseDouble(m.amount())))
                .orElse(null);

        List<DateRateSummary> calendar = buildCalendar(hotelId, checkIn, checkOut,
                rooms, displayCurrency, discount, fx);

        String searchToken = "st-" + hotelId + "-" + checkIn + "-" + checkOut + "-" + System.currentTimeMillis();
        return new AvailabilityResult(hotelId, checkIn, checkOut,
                new GuestCount(adults, children), displayCurrency,
                roomAvails, globalLowest, calendar, searchToken,
                OffsetDateTime.now().plusMinutes(30));
    }

    private List<DateRateSummary> buildCalendar(String hotelId, LocalDate start, LocalDate end,
                                                  Map<String, double[]> rooms, String displayCurrency,
                                                  double discount, double fx) {
        return start.datesUntil(end).map(date -> {
            double lowest = rooms.values().stream()
                    .mapToDouble(p -> p[0] * (1 - discount) * fx).min().orElse(0);
            return new DateRateSummary(date, Money.of(lowest, displayCurrency), displayCurrency, true);
        }).collect(Collectors.toList());
    }

    @Override
    public Optional<Rate> validateRate(String rateToken) {
        // rateToken format: rt-{rateId}-{checkIn}-{checkOut}
        if (rateToken == null || !rateToken.startsWith("rt-")) return Optional.empty();
        String[] parts = rateToken.split("-", 4);
        if (parts.length < 4) return Optional.empty();
        // Rate tokens are valid for 30 min — always valid in mock
        return Optional.empty(); // would re-build in real impl; stub returns empty for simplicity
    }

    @Override
    public Optional<Rate> findRateById(String id) {
        // Rates are ephemeral in new model; for entity fetcher compatibility return empty
        return Optional.empty();
    }

    @Override
    public Optional<RatePlan> findRatePlanById(String id) {
        return Optional.ofNullable(ratePlans.get(id));
    }

    @Override
    public List<Rate> findRatesByHotelId(String hotelId, LocalDate checkIn, LocalDate checkOut, int adults) {
        AvailabilityResult result = searchRates(hotelId, checkIn, checkOut, adults, 0, null,
                null, null, null, null);
        return result.getRoomAvailabilities().stream()
                .flatMap(ra -> ra.getRates().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<Rate> findRatesByRoomTypeId(String roomTypeId, LocalDate checkIn, LocalDate checkOut, int adults) {
        String hotelId = HOTEL_ROOMS.entrySet().stream()
                .filter(e -> e.getValue().containsKey(roomTypeId))
                .map(Map.Entry::getKey).findFirst()
                .orElseGet(() -> hotelIdFromFallbackRoomId(roomTypeId));
        if (hotelId == null) return List.of();
        AvailabilityResult result = searchRates(hotelId, checkIn, checkOut, adults, 0, null,
                null, List.of(roomTypeId), null, null);
        return result.getRoomAvailabilities().stream()
                .flatMap(ra -> ra.getRates().stream())
                .collect(Collectors.toList());
    }

    /**
     * Strip the trailing room suffix from a synthetic room ID to recover the
     * hotel ID. Mirrors the conventions in {@link #generateFallbackRooms}.
     */
    private static String hotelIdFromFallbackRoomId(String roomTypeId) {
        if (roomTypeId == null) return null;
        for (String suffix : List.of("-rm-dlx", "-rm-exe", "-rm-ste",
                                     "-rm1", "-rm2", "-rm3", "-rm4")) {
            if (roomTypeId.endsWith(suffix)) {
                return roomTypeId.substring(0, roomTypeId.length() - suffix.length());
            }
        }
        return null;
    }

    @Override
    public List<Promotion> findPromotions(String brandId, Boolean memberOnly) {
        return promotions.values().stream()
                .filter(p -> p.isActive())
                .filter(p -> brandId == null || brandId.equals(p.getBrandId()))
                .filter(p -> memberOnly == null || memberOnly.equals(p.isMemberOnly()))
                .filter(p -> !p.getValidTo().isBefore(LocalDate.now()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Promotion> findPromotionByCode(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(promotions.get(code.toUpperCase()));
    }

    @Override
    public Optional<Package> findPackageById(String id) {
        return Optional.ofNullable(packages.get(id));
    }

    @Override
    public List<DateRateSummary> getRateCalendar(String hotelId, LocalDate startDate,
                                                   LocalDate endDate, int adults, String currency) {
        Map<String, double[]> rooms = roomsForHotel(hotelId);
        String baseCurrency = HOTEL_CURRENCY.getOrDefault(hotelId, "USD");
        String displayCurrency = (currency != null && !currency.isBlank())
                ? currency.toUpperCase() : baseCurrency;
        double fx = convertCurrency(1.0, baseCurrency, displayCurrency);
        return buildCalendar(hotelId, startDate, endDate, rooms, displayCurrency, 0.0, fx);
    }

    @Override
    public Optional<GiftCardBalance> findGiftCardBalance(String code) {
        return Optional.ofNullable(giftCards.get(code != null ? code.toUpperCase() : null));
    }

    @Override
    public List<RedemptionRate> findRedemptionRates(String hotelId, LocalDate checkIn,
                                                     LocalDate checkOut, String roomTypeId) {
        Map<String, double[]> rooms = roomsForHotel(hotelId);
        return rooms.entrySet().stream()
                .filter(e -> roomTypeId == null || e.getKey().equals(roomTypeId))
                .map(e -> {
                    double pricePerNight = e.getValue()[0];
                    int pointsPerNight = (int) (e.getValue()[3]);
                    int nights = (int) checkIn.until(checkOut).getDays();
                    List<LocalDate> available = checkIn.datesUntil(checkOut).collect(Collectors.toList());
                    return new RedemptionRate(e.getKey(), hotelId, pointsPerNight * nights,
                            pointsPerNight, available, null);
                })
                .collect(Collectors.toList());
    }
}
