package com.luxe.property.datasource;

import com.luxe.property.schema.types.Brand;
import com.luxe.property.schema.types.DestinationSuggestion;
import com.luxe.property.schema.types.Hotel;
import com.luxe.property.schema.types.HotelFacets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hotel search, facet computation, and destination autocomplete — extracted
 * out of {@link PropertyMockDataSource} so the inventory store and the
 * search logic each have a single responsibility. Same subgraph, same
 * schema, same federation boundary; just a clean internal seam that makes
 * future migration to a real search engine (Elasticsearch / Algolia /
 * OpenSearch) straightforward — that day, this whole class becomes the
 * outbound port of a new {@code subgraph-search} service.
 *
 * <p>Constructor takes <em>references</em> to the live hotels and brands
 * maps in the data source, so seed data updates are reflected immediately
 * without copy-on-read overhead.
 */
public class PropertySearchService {

    private final Map<String, Hotel> hotels;
    private final Map<String, Brand> brands;

    public PropertySearchService(Map<String, Hotel> hotels, Map<String, Brand> brands) {
        this.hotels = hotels;
        this.brands = brands;
    }

    // ── Search ───────────────────────────────────────────────────────────

    public List<Hotel> searchHotels(Map<String, Object> filter, String sortBy) {
        List<Hotel> result = new ArrayList<>(hotels.values());
        if (filter != null) {
            String query = (String) filter.get("query");
            if (query != null) {
                String q = query.toLowerCase();
                result = result.stream().filter(h -> {
                    var addr = h.getLocation().address();
                    String name        = h.getName().toLowerCase();
                    String city        = addr.city() != null ? addr.city().toLowerCase() : "";
                    String state       = addr.state() != null ? addr.state().toLowerCase() : "";
                    String countryName = addr.countryName() != null ? addr.countryName().toLowerCase() : "";
                    String countryCode = addr.countryCode() != null ? addr.countryCode().toLowerCase() : "";
                    // Match the destination string against name, city, state,
                    // country name (substring), or country code (exact). The
                    // autocomplete fills the input with the display name when
                    // the user picks a city/state/country suggestion, so all
                    // four substring paths are load-bearing; country-code
                    // match also accepts ?destination=FR style URLs.
                    return name.contains(q)
                            || city.contains(q)
                            || state.contains(q)
                            || countryName.contains(q)
                            || countryCode.equals(q);
                }).collect(Collectors.toList());
            }
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) filter.get("ids");
            if (ids != null) {
                if (ids.isEmpty()) {
                    // Explicit empty list → match nothing. Distinct from null
                    // (which means "no id constraint").
                    result = List.of();
                } else {
                    Set<String> idSet = new HashSet<>(ids);
                    result = result.stream()
                            .filter(h -> idSet.contains(h.getId()))
                            .collect(Collectors.toList());
                }
            }
            @SuppressWarnings("unchecked")
            List<String> brandIds = (List<String>) filter.get("brandIds");
            if (brandIds != null && !brandIds.isEmpty()) {
                result = result.stream().filter(h -> brandIds.contains(h.getBrandId())).collect(Collectors.toList());
            }
            @SuppressWarnings("unchecked")
            List<String> countryCodes = (List<String>) filter.get("countryCodes");
            if (countryCodes != null && !countryCodes.isEmpty()) {
                result = result.stream()
                        .filter(h -> countryCodes.contains(h.getLocation().address().countryCode()))
                        .collect(Collectors.toList());
            }
            Integer minStar = (Integer) filter.get("minStarRating");
            if (minStar != null) result = result.stream().filter(h -> h.getStarRating() >= minStar).collect(Collectors.toList());
            Number minGuest = (Number) filter.get("minGuestRating");
            if (minGuest != null) {
                double threshold = minGuest.doubleValue();
                result = result.stream()
                        .filter(h -> h.getGuestRating() != null && h.getGuestRating().overall() >= threshold)
                        .collect(Collectors.toList());
            }
            if (Boolean.TRUE.equals(filter.get("hasSpa"))) result = result.stream().filter(Hotel::isHasSpa).collect(Collectors.toList());
            if (Boolean.TRUE.equals(filter.get("hasPool"))) result = result.stream().filter(Hotel::isHasPool).collect(Collectors.toList());
            if (Boolean.TRUE.equals(filter.get("hasGolf"))) result = result.stream().filter(Hotel::isHasGolf).collect(Collectors.toList());
            if (Boolean.TRUE.equals(filter.get("hasFreeBreakfast"))) result = result.stream().filter(Hotel::isHasFreeBreakfast).collect(Collectors.toList());
            if (Boolean.TRUE.equals(filter.get("petsAllowed"))) result = result.stream().filter(Hotel::isPetsAllowed).collect(Collectors.toList());
            // Price filter — uses an estimated USD-per-night derived from brand tier
            // and a deterministic seed. Matches the pricing subgraph's mock rates
            // closely enough for filter narrowing; the rate shown on the card still
            // comes from the pricing subgraph's per-hotel availability.
            Number minRate = (Number) filter.get("minNightlyRate");
            Number maxRate = (Number) filter.get("maxNightlyRate");
            if (minRate != null || maxRate != null) {
                final double lo = minRate != null ? minRate.doubleValue() : Double.NEGATIVE_INFINITY;
                final double hi = maxRate != null ? maxRate.doubleValue() : Double.POSITIVE_INFINITY;
                result = result.stream().filter(h -> {
                    double est = estimateNightlyRateUsd(h);
                    return est >= lo && est <= hi;
                }).collect(Collectors.toList());
            }
            @SuppressWarnings("unchecked")
            List<Object> tiers = (List<Object>) filter.get("brandTiers");
            if (tiers != null && !tiers.isEmpty()) {
                Set<String> tierNames = tiers.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.toSet());
                result = result.stream()
                        .filter(h -> {
                            Brand b = brands.get(h.getBrandId());
                            return b != null && tierNames.contains(b.getTier());
                        })
                        .collect(Collectors.toList());
            }
        }
        if (sortBy != null) {
            switch (sortBy) {
                case "GUEST_RATING" -> result.sort((a, b) -> Double.compare(
                        b.getGuestRating() != null ? b.getGuestRating().overall() : 0,
                        a.getGuestRating() != null ? a.getGuestRating().overall() : 0));
                case "STAR_RATING" -> result.sort((a, b) -> Integer.compare(b.getStarRating(), a.getStarRating()));
                case "REVIEWS" -> result.sort((a, b) -> Integer.compare(
                        b.getGuestRating() != null ? b.getGuestRating().count() : 0,
                        a.getGuestRating() != null ? a.getGuestRating().count() : 0));
                case "CITY" -> result.sort(Comparator
                        .comparing((Hotel h) -> h.getLocation().address().countryCode())
                        .thenComparing(h -> h.getLocation().address().city())
                        .thenComparing(Hotel::getName));
                case "BRAND" -> result.sort(Comparator
                        .comparing((Hotel h) -> {
                            Brand br = brands.get(h.getBrandId());
                            return br != null ? br.getName() : "";
                        })
                        .thenComparing(Hotel::getName));
                case "PRICE_LOW_TO_HIGH" -> result.sort(
                        Comparator.comparingDouble(this::estimateNightlyRateUsd));
                case "DISTANCE" -> sortByDistanceFromCentroid(result);
                case "NAME" -> result.sort(Comparator.comparing(Hotel::getName));
                default -> { /* unknown sortBy → leave default order */ }
            }
        }
        return result;
    }

    // ── Facets ───────────────────────────────────────────────────────────

    public HotelFacets computeFacets(Map<String, Object> filter) {
        // Apply the current filter EXCEPT the dimension being faceted, so
        // each option's count reflects "how many hotels remain if I check this
        // additionally" (the user-friendly drill-down semantic).
        List<Hotel> baseSet = searchHotels(filter, null);

        // Per-brand
        Map<String, Integer> brandCounts = new LinkedHashMap<>();
        for (Hotel h : searchHotels(omit(filter, "brandIds"), null)) {
            brandCounts.merge(h.getBrandId(), 1, Integer::sum);
        }
        List<HotelFacets.BrandFacet> byBrand = brandCounts.entrySet().stream()
                .map(e -> new HotelFacets.BrandFacet(e.getKey(), brands.get(e.getKey()), e.getValue()))
                .filter(f -> f.brand() != null)
                .sorted(Comparator
                        .<HotelFacets.BrandFacet>comparingInt(f -> -f.count())
                        .thenComparing(f -> f.brand().getName()))
                .toList();

        // Per-tier
        Map<String, Integer> tierCounts = new LinkedHashMap<>();
        for (Hotel h : searchHotels(omit(filter, "brandTiers"), null)) {
            Brand b = brands.get(h.getBrandId());
            if (b != null && b.getTier() != null) tierCounts.merge(b.getTier(), 1, Integer::sum);
        }
        List<HotelFacets.TierFacet> byTier = List.of("LUXURY", "PREMIUM", "SELECT").stream()
                .map(t -> new HotelFacets.TierFacet(t, tierCounts.getOrDefault(t, 0)))
                .toList();

        // Per-city — use the BASE set (city is informational, not a filter dim).
        Map<String, int[]> cityCounts = new LinkedHashMap<>();
        Map<String, String> cityCountry = new HashMap<>();
        for (Hotel h : baseSet) {
            String city = h.getLocation().address().city();
            String cc = h.getLocation().address().countryCode();
            cityCounts.computeIfAbsent(city, k -> new int[1])[0]++;
            cityCountry.putIfAbsent(city, cc);
        }
        List<HotelFacets.CityFacet> byCity = cityCounts.entrySet().stream()
                .map(e -> new HotelFacets.CityFacet(e.getKey(), cityCountry.get(e.getKey()), e.getValue()[0]))
                .sorted(Comparator
                        .<HotelFacets.CityFacet>comparingInt(c -> -c.count())
                        .thenComparing(HotelFacets.CityFacet::city))
                .toList();

        // Per-amenity (each computed with everything else in filter applied,
        // but the amenity dim itself omitted).
        Map<String, Object> sansAmen = omit(filter, "hasFreeBreakfast", "hasPool", "hasSpa", "hasGolf", "petsAllowed");
        List<Hotel> amenSet = searchHotels(sansAmen, null);
        int hasFreeBreakfast = (int) amenSet.stream().filter(Hotel::isHasFreeBreakfast).count();
        int hasPool = (int) amenSet.stream().filter(Hotel::isHasPool).count();
        int hasSpa = (int) amenSet.stream().filter(Hotel::isHasSpa).count();
        int hasGolf = (int) amenSet.stream().filter(Hotel::isHasGolf).count();
        int petsAllowed = (int) amenSet.stream().filter(Hotel::isPetsAllowed).count();
        HotelFacets.AmenityFacets amenities = new HotelFacets.AmenityFacets(
                hasFreeBreakfast, hasPool, hasSpa, hasGolf, petsAllowed);

        // Per-guest-rating bucket — counts at each threshold (≥7, ≥8, ≥9).
        List<Hotel> grBase = searchHotels(omit(filter, "minGuestRating"), null);
        List<HotelFacets.GuestRatingBucket> guestRating = List.of(9.0, 8.0, 7.0).stream()
                .map(min -> new HotelFacets.GuestRatingBucket(
                        min,
                        (int) grBase.stream()
                                .filter(h -> h.getGuestRating() != null && h.getGuestRating().overall() >= min)
                                .count()))
                .toList();

        return new HotelFacets(baseSet.size(), byBrand, byTier, byCity, amenities, guestRating);
    }

    // ── Destination autocomplete ─────────────────────────────────────────

    public List<DestinationSuggestion> destinationSuggestions(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) return List.of();
        String q = query.trim().toLowerCase();
        int cap = Math.min(limit, 25);

        // Walk every hotel once; stash unique cities, states, and countries
        // with their hotel counts as we go. We rank in two passes: prefix
        // matches first (more relevant), substring matches second.
        record CityKey(String city, String country, String countryCode) {}
        record StateKey(String state, String country, String countryCode) {}
        Map<CityKey, Integer> cityCounts = new LinkedHashMap<>();
        Map<StateKey, Integer> stateCounts = new LinkedHashMap<>();
        Map<String, int[]> countryCounts = new LinkedHashMap<>();
        Map<String, String> countryCodeByName = new HashMap<>();
        List<Hotel> hotelMatchesPrefix = new ArrayList<>();
        List<Hotel> hotelMatchesSubstring = new ArrayList<>();

        for (Hotel h : hotels.values()) {
            String name = h.getName().toLowerCase();
            String city = h.getLocation().address().city();
            String state = h.getLocation().address().state();
            String country = h.getLocation().address().countryName();
            String countryCode = h.getLocation().address().countryCode();

            // Tally cities + states + countries (every hotel contributes once).
            cityCounts.merge(new CityKey(city, country, countryCode), 1, Integer::sum);
            if (state != null && !state.isBlank()) {
                stateCounts.merge(new StateKey(state, country, countryCode), 1, Integer::sum);
            }
            if (country != null) {
                countryCounts.computeIfAbsent(country, k -> new int[1])[0]++;
                countryCodeByName.put(country, countryCode);
            }

            // Hotel-level match.
            if (name.startsWith(q)) hotelMatchesPrefix.add(h);
            else if (name.contains(q)) hotelMatchesSubstring.add(h);
        }

        // Cities matching the query (prefix > substring).
        List<DestinationSuggestion> cityPrefix = new ArrayList<>();
        List<DestinationSuggestion> citySubstring = new ArrayList<>();
        for (var entry : cityCounts.entrySet()) {
            String name = entry.getKey().city().toLowerCase();
            var s = DestinationSuggestion.city(
                    entry.getKey().city(), entry.getKey().country(),
                    entry.getKey().countryCode(), entry.getValue());
            if (name.startsWith(q)) cityPrefix.add(s);
            else if (name.contains(q)) citySubstring.add(s);
        }

        // States matching the query (prefix > substring).
        List<DestinationSuggestion> statePrefix = new ArrayList<>();
        List<DestinationSuggestion> stateSubstring = new ArrayList<>();
        for (var entry : stateCounts.entrySet()) {
            String name = entry.getKey().state().toLowerCase();
            var s = DestinationSuggestion.state(
                    entry.getKey().state(), entry.getKey().country(),
                    entry.getKey().countryCode(), entry.getValue());
            if (name.startsWith(q)) statePrefix.add(s);
            else if (name.contains(q)) stateSubstring.add(s);
        }

        // Countries matching the query (prefix > substring).
        List<DestinationSuggestion> countryPrefix = new ArrayList<>();
        List<DestinationSuggestion> countrySubstring = new ArrayList<>();
        for (var entry : countryCounts.entrySet()) {
            String name = entry.getKey().toLowerCase();
            var s = DestinationSuggestion.country(
                    entry.getKey(), countryCodeByName.get(entry.getKey()), entry.getValue()[0]);
            if (name.startsWith(q)) countryPrefix.add(s);
            else if (name.contains(q)) countrySubstring.add(s);
        }

        // Final ranking: cities (specific intent) → states (regional intent)
        // → countries (broadest) → hotels (specific). Prefix matches always
        // come before substring matches within the same tier.
        Comparator<DestinationSuggestion> byLabel =
                Comparator.comparing(DestinationSuggestion::label);
        cityPrefix.sort(byLabel);
        citySubstring.sort(byLabel);
        statePrefix.sort(byLabel);
        stateSubstring.sort(byLabel);
        countryPrefix.sort(byLabel);
        countrySubstring.sort(byLabel);
        hotelMatchesPrefix.sort(Comparator.comparing(Hotel::getName));
        hotelMatchesSubstring.sort(Comparator.comparing(Hotel::getName));

        List<DestinationSuggestion> out = new ArrayList<>(cap);
        appendUntilFull(out, cityPrefix, cap);
        appendUntilFull(out, statePrefix, cap);
        appendUntilFull(out, countryPrefix, cap);
        for (Hotel h : hotelMatchesPrefix) {
            if (out.size() >= cap) break;
            out.add(DestinationSuggestion.hotel(h));
        }
        appendUntilFull(out, citySubstring, cap);
        appendUntilFull(out, stateSubstring, cap);
        appendUntilFull(out, countrySubstring, cap);
        for (Hotel h : hotelMatchesSubstring) {
            if (out.size() >= cap) break;
            out.add(DestinationSuggestion.hotel(h));
        }
        return out;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Return a copy of {@code filter} with the given keys removed. Null-safe. */
    static Map<String, Object> omit(Map<String, Object> filter, String... keys) {
        Map<String, Object> out = filter == null ? new HashMap<>() : new HashMap<>(filter);
        for (String k : keys) out.remove(k);
        return out;
    }

    /**
     * Sort the result list ascending by haversine distance from the centroid
     * of the matching hotels. When the user has a destination filter, the
     * centroid effectively sits at the heart of the filtered cluster
     * (e.g. central Paris when destination=Paris), so the closest match
     * comes first — the standard "Distance" sort users expect.
     */
    static void sortByDistanceFromCentroid(List<Hotel> result) {
        if (result.size() <= 1) return;
        double avgLat = result.stream()
                .mapToDouble(h -> h.getLocation().coordinates().latitude())
                .average().orElse(0);
        double avgLng = result.stream()
                .mapToDouble(h -> h.getLocation().coordinates().longitude())
                .average().orElse(0);
        result.sort(Comparator.comparingDouble(h -> haversineKm(
                h.getLocation().coordinates().latitude(),
                h.getLocation().coordinates().longitude(),
                avgLat, avgLng)));
    }

    /** Great-circle distance in kilometres between two lat/lng pairs. */
    static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Deterministic per-hotel price estimate in USD/night used for the price
     * range filter. Buckets by brand tier with a seed-driven jitter so each
     * hotel sits at a stable position inside its band.
     *
     *   LUXURY  : 400 – 1500
     *   PREMIUM : 200 –  500
     *   SELECT  : 100 –  250
     */
    double estimateNightlyRateUsd(Hotel h) {
        Brand b = brands.get(h.getBrandId());
        String tier = b != null ? b.getTier() : "PREMIUM";
        int seed = Math.abs(h.getId().hashCode());
        return switch (tier) {
            case "LUXURY"  -> 400 + (seed % 1100);
            case "PREMIUM" -> 200 + (seed %  300);
            case "SELECT"  -> 100 + (seed %  150);
            default        -> 250;
        };
    }

    private static void appendUntilFull(
            List<DestinationSuggestion> sink,
            List<DestinationSuggestion> from,
            int cap) {
        for (var s : from) {
            if (sink.size() >= cap) return;
            sink.add(s);
        }
    }
}
