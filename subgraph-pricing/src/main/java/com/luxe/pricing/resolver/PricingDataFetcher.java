package com.luxe.pricing.resolver;

import com.luxe.pricing.schema.types.RoomType;

import com.luxe.pricing.schema.types.Hotel;

import com.luxe.common.config.CachingConfig;
import com.luxe.common.error.NotFoundError;
import com.luxe.pricing.datasource.PricingDataSource;
import com.luxe.pricing.schema.types.*;
import com.netflix.graphql.dgs.*;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@DgsComponent
public class PricingDataFetcher {

    private final PricingDataSource dataSource;

    public PricingDataFetcher(PricingDataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @DgsQuery
    public Object searchRates(@InputArgument Map<String, Object> input) {
        String hotelId = (String) input.get("hotelId");
        if (hotelId == null) return new com.luxe.common.error.ValidationError(
                "MISSING_FIELD", "hotelId is required",
                List.of(new com.luxe.common.error.FieldError("hotelId", "Required")));

        LocalDate checkIn  = toDate(input.get("checkIn"));
        LocalDate checkOut = toDate(input.get("checkOut"));

        if (!checkIn.isBefore(checkOut)) {
            return new com.luxe.common.error.ValidationError(
                    "INVALID_DATES", "checkOut must be after checkIn",
                    List.of(new com.luxe.common.error.FieldError("checkOut", "Must be after checkIn")));
        }

        int adults   = input.containsKey("adults") ? ((Number) input.get("adults")).intValue() : 1;
        int children = input.containsKey("children") ? ((Number) input.get("children")).intValue() : 0;
        String currency = (String) input.get("currency");

        @SuppressWarnings("unchecked")
        List<String> ratePlanCodes = (List<String>) input.get("ratePlanCodes");
        @SuppressWarnings("unchecked")
        List<String> roomTypeIds = (List<String>) input.get("roomTypeIds");
        String promoCode    = (String) input.get("promoCode");
        String corpCode     = (String) input.get("corporateCode");

        return dataSource.searchRates(hotelId, checkIn, checkOut, adults, children, currency,
                ratePlanCodes, roomTypeIds, promoCode, corpCode);
    }

    @DgsQuery
    public Object validateRate(@InputArgument String rateToken) {
        return dataSource.validateRate(rateToken)
                .<Object>map(r -> r)
                .orElse(new com.luxe.common.error.RateExpiredError("Rate token is expired or invalid"));
    }

    @DgsQuery
    public Object ratePlan(@InputArgument String id) {
        return dataSource.findRatePlanById(id).orElse(null);
    }

    @DgsQuery
    public List<Promotion> promotions(@InputArgument Integer first, @InputArgument String after,
                                       @InputArgument List<String> brandIds,
                                       @InputArgument Boolean memberOnly) {
        String brandId = (brandIds != null && !brandIds.isEmpty()) ? brandIds.get(0) : null;
        List<Promotion> all = dataSource.findPromotions(brandId, memberOnly);
        int limit = first != null ? first : all.size();
        return all.stream().limit(limit).toList();
    }

    @DgsQuery
    public Object promotion(@InputArgument String code) {
        return dataSource.findPromotionByCode(code).orElse(null);
    }

    @DgsQuery(field = "package")
    public Object getPackage(@InputArgument String id) {
        return dataSource.findPackageById(id).orElse(null);
    }

    @DgsQuery
    public List<DateRateSummary> rateCalendar(@InputArgument Map<String, Object> input) {
        String hotelId   = (String) input.get("hotelId");
        LocalDate start  = toDate(input.get("startDate"));
        LocalDate end    = toDate(input.get("endDate"));
        int adults       = input.containsKey("adults") ? ((Number) input.get("adults")).intValue() : 1;
        String currency  = (String) input.getOrDefault("currency", "USD");
        return dataSource.getRateCalendar(hotelId, start, end, adults, currency);
    }

    @DgsQuery
    public GiftCardBalance giftCardBalance(@InputArgument String code) {
        // Schema returns nullable GiftCardBalance (no union) — return null on miss.
        return dataSource.findGiftCardBalance(code).orElse(null);
    }

    @DgsQuery
    public List<RedemptionRate> redemptionRates(@InputArgument Map<String, Object> input) {
        String hotelId   = (String) input.get("hotelId");
        LocalDate checkIn  = toDate(input.get("checkIn"));
        LocalDate checkOut = toDate(input.get("checkOut"));
        String roomTypeId  = (String) input.get("roomTypeId");
        return dataSource.findRedemptionRates(hotelId, checkIn, checkOut, roomTypeId);
    }

    /**
     * Catalogue of special-rate options exposed on the home-page
     * search bar. Hard-coded rather than seeded — the labels +
     * descriptions are partner-facing product copy that wouldn't
     * change at the row level; in a real deployment they'd live in
     * a CMS, but the schema contract stays the same.
     */
    // 5-entry hard-coded catalogue — perfect cache target. Every
    // home / search / brand / rates / book / confirmation page pulls
    // this; 1 h TTL keeps the resolver cold past the warm-up.
    @DgsQuery
    @Cacheable(CachingConfig.PRICING_SPECIAL_RATES)
    public List<Map<String, Object>> specialRates() {
        return List.of(
                Map.of("code", "BEST_AVAILABLE",
                       "label", "Lowest Regular Rate",
                       "description", "Best publicly-available rate, no membership required.",
                       "requiresCode", false),
                Map.of("code", "AAA_CAA",
                       "label", "AAA/CAA Discount",
                       "description", "Member savings for AAA (US) and CAA (Canada) cardholders.",
                       "requiresCode", false),
                Map.of("code", "SENIOR",
                       "label", "Senior Discount",
                       "description", "Reduced rate for guests aged 62 and over. Valid ID required at check-in.",
                       "requiresCode", false),
                Map.of("code", "GOVERNMENT",
                       "label", "Government / Military",
                       "description", "Per-diem-aligned rate for active government and military personnel.",
                       "requiresCode", false),
                Map.of("code", "CORPORATE",
                       "label", "Corp / Promo Code",
                       "description", "Apply your employer's negotiated rate or a promotional code.",
                       "requiresCode", true));
    }

    // ── Hotel field resolvers ─────────────────────────────────────────────────

    @DgsData(parentType = "Hotel", field = "rates")
    public List<Rate> hotelRates(DataFetchingEnvironment dfe) {
        Hotel source = dfe.getSource();
        String hotelId = source.getId();
        LocalDate checkIn  = toDate(dfe.getArgument("checkIn"));
        LocalDate checkOut = toDate(dfe.getArgument("checkOut"));
        int adults = dfe.getArgumentOrDefault("adults", 1);
        return dataSource.findRatesByHotelId(hotelId, checkIn, checkOut, adults);
    }

    @DgsData(parentType = "Hotel", field = "availability")
    public AvailabilityResult hotelAvailability(DataFetchingEnvironment dfe) {
        Hotel source = dfe.getSource();
        String hotelId = source.getId();
        LocalDate checkIn  = toDate(dfe.getArgument("checkIn"));
        LocalDate checkOut = toDate(dfe.getArgument("checkOut"));
        int adults   = dfe.getArgumentOrDefault("adults", 1);
        int children = dfe.getArgumentOrDefault("children", 0);
        String currency = dfe.getArgumentOrDefault("currency", "USD");
        return dataSource.searchRates(hotelId, checkIn, checkOut, adults, children, currency,
                null, null, null, null);
    }

    // ── RoomType field resolvers ──────────────────────────────────────────────

    @DgsData(parentType = "RoomType", field = "rates")
    public List<Rate> roomTypeRates(DataFetchingEnvironment dfe) {
        RoomType source = dfe.getSource();
        String roomTypeId = source.getId();
        LocalDate checkIn  = toDate(dfe.getArgument("checkIn"));
        LocalDate checkOut = toDate(dfe.getArgument("checkOut"));
        int adults = dfe.getArgumentOrDefault("adults", 1);
        return dataSource.findRatesByRoomTypeId(roomTypeId, checkIn, checkOut, adults);
    }

    @DgsData(parentType = "RoomType", field = "availability")
    public Object roomTypeAvailability(DataFetchingEnvironment dfe) {
        RoomType source = dfe.getSource();
        String roomTypeId = source.getId();
        LocalDate checkIn  = toDate(dfe.getArgument("checkIn"));
        LocalDate checkOut = toDate(dfe.getArgument("checkOut"));
        List<Rate> rates = dataSource.findRatesByRoomTypeId(roomTypeId, checkIn, checkOut, 1);
        if (rates.isEmpty()) return null;
        Rate lowest = rates.stream()
                .min(java.util.Comparator.comparingDouble(r -> Double.parseDouble(r.getTotalRate().amount())))
                .orElse(null);
        int count = 1 + (int) (Math.abs(roomTypeId.hashCode()) % 5);
        return new RoomAvailability(roomTypeId, rates, lowest, count, null);
    }

    // ── Entity Fetchers ───────────────────────────────────────────────────────

    @DgsEntityFetcher(name = "Rate")
    public Rate fetchRate(Map<String, Object> values) {
        return dataSource.findRateById((String) values.get("id")).orElse(null);
    }

    @DgsEntityFetcher(name = "RatePlan")
    public RatePlan fetchRatePlan(Map<String, Object> values) {
        return dataSource.findRatePlanById((String) values.get("id")).orElse(null);
    }

    @DgsEntityFetcher(name = "Promotion")
    public Promotion fetchPromotion(Map<String, Object> values) {
        return dataSource.findPromotionByCode((String) values.get("code")).orElse(null);
    }
    @DgsEntityFetcher(name = "Hotel")
    public Hotel fetchHotelReference(Map<String, Object> values) {
        return new Hotel((String) values.get("id"));
    }

    @DgsEntityFetcher(name = "RoomType")
    public RoomType fetchRoomTypeReference(Map<String, Object> values) {
        return new RoomType((String) values.get("id"));
    }

    /**
     * Coerce a Date-scalar argument that DGS may have already deserialised into
     * a {@link LocalDate}, but which legacy code paths sometimes still receive
     * as a String. Returning {@code null} for null lets callers decide whether
     * the field is required.
     */
    private static LocalDate toDate(Object raw) {
        if (raw == null) return null;
        return raw instanceof LocalDate ld ? ld : LocalDate.parse(raw.toString());
    }
}
