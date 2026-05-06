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

    // hotelId -> roomTypeId -> base nightly price (in cents, for the currency)
    private static final Map<String, Map<String, double[]>> HOTEL_ROOMS = new LinkedHashMap<>();

    static {
        // [basePriceFlexible, basePriceNonRefundable, currency_multiplier, pointsPerNight]
        // currency stored separately per hotel
        HOTEL_ROOMS.put("prop-paris-001", new LinkedHashMap<>(Map.of(
                "rt-paris-std-001",   new double[]{350, 280, 1.0, 1000},
                "rt-paris-dlx-001",   new double[]{520, 415, 1.0, 1500},
                "rt-paris-suite-001", new double[]{720, 576, 1.0, 2500}
        )));
        HOTEL_ROOMS.put("prop-tokyo-001", new LinkedHashMap<>(Map.of(
                "rt-tokyo-std-001",   new double[]{55000, 44000, 1.0, 800},
                "rt-tokyo-dlx-001",   new double[]{75000, 60000, 1.0, 1200},
                "rt-tokyo-suite-001", new double[]{180000, 144000, 1.0, 3000}
        )));
        HOTEL_ROOMS.put("prop-dubai-001", new LinkedHashMap<>(Map.of(
                "rt-dubai-dlx-001",   new double[]{1100, 880, 1.0, 1500},
                "rt-dubai-suite-001", new double[]{3800, 3040, 1.0, 5000},
                "rt-dubai-villa-001", new double[]{11000, 8800, 1.0, 15000}
        )));
        HOTEL_ROOMS.put("prop-nyc-001", new LinkedHashMap<>(Map.of(
                "rt-nyc-std-001",   new double[]{550, 440, 1.0, 1200},
                "rt-nyc-dlx-001",   new double[]{720, 576, 1.0, 1800},
                "rt-nyc-suite-001", new double[]{4500, 3600, 1.0, 8000}
        )));
        HOTEL_ROOMS.put("prop-london-001", new LinkedHashMap<>(Map.of(
                "rt-london-std-001",   new double[]{380, 304, 1.0, 1000},
                "rt-london-dlx-001",   new double[]{580, 464, 1.0, 1600},
                "rt-london-suite-001", new double[]{3200, 2560, 1.0, 7000}
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
        add(new RatePlan("rp-bar", "BAR", "Best Available Rate", "BEST_AVAILABLE",
                "Our best flexible rate with free cancellation", true, false, true, false,
                true, 1.0, FREE_48H, 1, null, null));
        add(new RatePlan("rp-nr", "NR", "Non-Refundable Saver", "BEST_AVAILABLE",
                "Lowest price — non-refundable, no changes", false, false, true, false,
                true, 1.0, NON_REF, 1, null, null));
        add(new RatePlan("rp-bb", "BB", "Bed & Breakfast", "BEST_AVAILABLE",
                "Room rate includes daily breakfast for two", true, true, true, false,
                true, 1.2, FREE_72H, 2, null, null));
        add(new RatePlan("rp-member", "MEMBER", "Luxe Member Rate", "MEMBER_RATE",
                "Exclusive rate for Luxe loyalty members — 15% off BAR", true, false, true, false,
                true, 1.5, FREE_48H, 1, null, 0));
        add(new RatePlan("rp-advance30", "ADV30", "Advance Purchase 30", "ADVANCE_PURCHASE",
                "Save 20% when booking 30+ days in advance", false, false, true, false,
                true, 1.0, NON_REF, 1, null, 30));
        add(new RatePlan("rp-corp", "CORP", "Corporate Rate", "CORPORATE",
                "Negotiated rate for corporate accounts", true, false, true, true,
                true, 1.0, FREE_48H, 1, null, 1));
        add(new RatePlan("rp-pkg-spa", "PKG-SPA", "Spa & Stay Package", "PACKAGE",
                "Room plus daily spa access for two", true, false, true, false,
                false, null, FREE_72H, 2, null, null));
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

    // ── PricingDataSource ─────────────────────────────────────────────────────

    @Override
    public AvailabilityResult searchRates(String hotelId, LocalDate checkIn, LocalDate checkOut,
                                           int adults, int children, String currency,
                                           List<String> ratePlanCodes, List<String> roomTypeIds,
                                           String promoCode, String corporateCode) {
        Map<String, double[]> rooms = HOTEL_ROOMS.getOrDefault(hotelId, Map.of());
        String hotelCurrency = HOTEL_CURRENCY.getOrDefault(hotelId, currency != null ? currency : "USD");
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
                    double flexBase = prices[0] * (1 - discount);
                    double nrBase   = prices[1] * (1 - discount);

                    List<RatePlan> plans = new ArrayList<>(List.of(
                            ratePlans.get("rp-bar"), ratePlans.get("rp-nr"),
                            ratePlans.get("rp-bb")));
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
                        Money strike = plan.isRefundable() ? null : Money.of(flexBase, hotelCurrency);
                        String rateId = "r-" + rtId + "-" + plan.getCode().toLowerCase() + "-"
                                + checkIn + "-" + checkOut;
                        rates.add(buildRate(rateId, hotelId, rtId, plan, base,
                                hotelCurrency, taxRate, checkIn, checkOut, adults, strike));
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
                rooms, hotelCurrency, discount);

        String searchToken = "st-" + hotelId + "-" + checkIn + "-" + checkOut + "-" + System.currentTimeMillis();
        return new AvailabilityResult(hotelId, checkIn, checkOut,
                new GuestCount(adults, children), hotelCurrency,
                roomAvails, globalLowest, calendar, searchToken,
                OffsetDateTime.now().plusMinutes(30));
    }

    private List<DateRateSummary> buildCalendar(String hotelId, LocalDate start, LocalDate end,
                                                  Map<String, double[]> rooms, String currency,
                                                  double discount) {
        return start.datesUntil(end).map(date -> {
            double lowest = rooms.values().stream()
                    .mapToDouble(p -> p[0] * (1 - discount)).min().orElse(0);
            return new DateRateSummary(date, Money.of(lowest, currency), currency, true);
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
                .map(Map.Entry::getKey).findFirst().orElse(null);
        if (hotelId == null) return List.of();
        AvailabilityResult result = searchRates(hotelId, checkIn, checkOut, adults, 0, null,
                null, List.of(roomTypeId), null, null);
        return result.getRoomAvailabilities().stream()
                .flatMap(ra -> ra.getRates().stream())
                .collect(Collectors.toList());
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
        Map<String, double[]> rooms = HOTEL_ROOMS.getOrDefault(hotelId, Map.of());
        String hotelCurrency = HOTEL_CURRENCY.getOrDefault(hotelId, currency != null ? currency : "USD");
        return buildCalendar(hotelId, startDate, endDate, rooms, hotelCurrency, 0.0);
    }

    @Override
    public Optional<GiftCardBalance> findGiftCardBalance(String code) {
        return Optional.ofNullable(giftCards.get(code != null ? code.toUpperCase() : null));
    }

    @Override
    public List<RedemptionRate> findRedemptionRates(String hotelId, LocalDate checkIn,
                                                     LocalDate checkOut, String roomTypeId) {
        Map<String, double[]> rooms = HOTEL_ROOMS.getOrDefault(hotelId, Map.of());
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
