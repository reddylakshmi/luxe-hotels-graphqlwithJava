package com.luxe.property.datasource;

import com.luxe.property.schema.types.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Procedurally generates the bulk of the chain's properties so the demo can
 * showcase a multi-brand, multi-country portfolio without thousands of lines
 * of hand-typed data. Output is deterministic — every startup produces the
 * same hotels — so links across other subgraphs keep resolving.
 *
 * The 5 rich hotels seeded by {@link PropertyMockDataSource} (Paris, London,
 * Tokyo, Dubai, NYC under the {@code Luxe Collection} brand) are intentionally
 * left in place; this generator adds 19 sibling brands with templated hotels
 * around them.
 */
final class PropertyDataGenerator {

    record Out(List<Brand> brands, List<Hotel> hotels, List<RoomType> rooms) {}

    /** Brand archetypes. Tier counts: 4 LUXURY (in addition to LUX) + 8 PREMIUM + 7 SELECT = 19. */
    private record BrandSpec(String code, String name, String slug, String tier,
                              String tagline, String accent, double pointsMultiplier) {}

    private static final List<BrandSpec> BRANDS = List.of(
            // LUXURY ─────────────────────────────────────────────
            new BrandSpec("MAI", "Maison Lumière",     "maison-lumiere",      "LUXURY",  "French art-de-vivre at hotel scale.",        "#8E5A3F", 3.0),
            new BrandSpec("ATL", "Atelier Hotels",     "atelier-hotels",      "LUXURY",  "Design-forward urban boutique luxury.",      "#2E2A26", 3.0),
            new BrandSpec("AUR", "Aurelia Resorts",    "aurelia-resorts",     "LUXURY",  "Mediterranean coastal sanctuaries.",         "#B89B7A", 3.0),
            new BrandSpec("RGL", "Regalia Grand",      "regalia-grand",       "LUXURY",  "Classic grand hotels of the world.",         "#5A3D2B", 3.0),

            // PREMIUM ────────────────────────────────────────────
            new BrandSpec("SOL", "Solstice Hotels",    "solstice-hotels",     "PREMIUM", "Elevated comfort, refined service.",         "#D8A656", 2.0),
            new BrandSpec("HRT", "Heritage Mansions",  "heritage-mansions",   "PREMIUM", "Historic homes, contemporary stays.",        "#4F3A29", 2.0),
            new BrandSpec("EMB", "Ember & Oak",        "ember-and-oak",       "PREMIUM", "Lodge-style retreats and hideaways.",        "#7A4A2A", 2.0),
            new BrandSpec("VRD", "Verdé Collection",   "verde-collection",    "PREMIUM", "Eco-premium, regenerative travel.",          "#4F6B47", 2.5),
            new BrandSpec("NST", "Northstar Hotels",   "northstar-hotels",    "PREMIUM", "Urban sophistication for the modern traveller.", "#3E5066", 2.0),
            new BrandSpec("PVL", "Pavilion Resorts",   "pavilion-resorts",    "PREMIUM", "Tropical resorts and beach escapes.",        "#C77B4F", 2.0),
            new BrandSpec("ISO", "Isola Living",       "isola-living",        "PREMIUM", "Italian island-life inspired stays.",        "#8B5742", 2.0),
            new BrandSpec("MNT", "Montane Lodges",     "montane-lodges",      "PREMIUM", "Mountain lodges and alpine retreats.",       "#5C6E5E", 2.0),

            // SELECT ─────────────────────────────────────────────
            new BrandSpec("MQS", "Marquis Lifestyle",  "marquis-lifestyle",   "SELECT",  "Modern lifestyle hotels for discerning travellers.", "#8C7B6B", 1.5),
            new BrandSpec("LUM", "Lume Living",        "lume-living",         "SELECT",  "Wellness-focused select-service stays.",     "#B8B0A3", 1.5),
            new BrandSpec("QLB", "Quill Boutique",     "quill-boutique",      "SELECT",  "Literary boutique hotels in walkable neighbourhoods.", "#5C4A3D", 1.5),
            new BrandSpec("WAY", "Wayfarer Hotels",    "wayfarer-hotels",     "SELECT",  "Smart hotels for the world's wayfarers.",    "#7B6450", 1.0),
            new BrandSpec("CRD", "Cardinal Inns",      "cardinal-inns",       "SELECT",  "Reliable mid-scale comfort, everywhere you go.", "#9C3D3D", 1.0),
            new BrandSpec("HRH", "Hearth Residences",  "hearth-residences",   "SELECT",  "Long-stay residences that feel like home.",  "#6B5840", 1.0),
            new BrandSpec("WST", "Westwind Studios",   "westwind-studios",    "SELECT",  "Smart urban studios for short or long stays.", "#8C9A8C", 1.0)
    );

    /** Major cities per country, with rough lat/lng for the hotel coordinates. */
    private record CitySeed(String name, double lat, double lng, String tz) {}
    private record CountrySeed(String code, String name, String region, String currency, List<CitySeed> cities) {}

    private static CitySeed c(String name, double lat, double lng, String tz) { return new CitySeed(name, lat, lng, tz); }

    private static final List<CountrySeed> COUNTRIES = List.of(
            // North America
            new CountrySeed("US", "United States",          "North America",  "USD", List.of(
                    c("New York",      40.7128, -74.0060, "America/New_York"),
                    c("Los Angeles",   34.0522, -118.2437, "America/Los_Angeles"),
                    c("Chicago",       41.8781, -87.6298, "America/Chicago"),
                    c("Miami",         25.7617, -80.1918, "America/New_York"),
                    c("San Francisco", 37.7749, -122.4194, "America/Los_Angeles"),
                    c("Boston",        42.3601, -71.0589, "America/New_York"),
                    c("Seattle",       47.6062, -122.3321, "America/Los_Angeles"),
                    c("Las Vegas",     36.1699, -115.1398, "America/Los_Angeles"))),
            new CountrySeed("CA", "Canada",                 "North America",  "CAD", List.of(
                    c("Toronto",       43.6532, -79.3832,  "America/Toronto"),
                    c("Vancouver",     49.2827, -123.1207, "America/Vancouver"),
                    c("Montréal",      45.5019, -73.5674,  "America/Toronto"),
                    c("Calgary",       51.0447, -114.0719, "America/Edmonton"))),
            new CountrySeed("MX", "Mexico",                 "North America",  "MXN", List.of(
                    c("Mexico City",   19.4326, -99.1332,  "America/Mexico_City"),
                    c("Cancún",        21.1619, -86.8515,  "America/Cancun"),
                    c("Tulum",         20.2114, -87.4654,  "America/Cancun"))),

            // Western Europe
            new CountrySeed("GB", "United Kingdom",         "Western Europe", "GBP", List.of(
                    c("London",        51.5074, -0.1278,   "Europe/London"),
                    c("Edinburgh",     55.9533, -3.1883,   "Europe/London"),
                    c("Manchester",    53.4808, -2.2426,   "Europe/London"),
                    c("Bath",          51.3811, -2.3590,   "Europe/London"))),
            new CountrySeed("FR", "France",                 "Western Europe", "EUR", List.of(
                    c("Paris",         48.8566, 2.3522,    "Europe/Paris"),
                    c("Cannes",        43.5528, 7.0174,    "Europe/Paris"),
                    c("Nice",          43.7102, 7.2620,    "Europe/Paris"),
                    c("Bordeaux",      44.8378, -0.5792,   "Europe/Paris"),
                    c("Lyon",          45.7640, 4.8357,    "Europe/Paris"))),
            new CountrySeed("DE", "Germany",                "Western Europe", "EUR", List.of(
                    c("Berlin",        52.5200, 13.4050,   "Europe/Berlin"),
                    c("Munich",        48.1351, 11.5820,   "Europe/Berlin"),
                    c("Hamburg",       53.5511, 9.9937,    "Europe/Berlin"),
                    c("Frankfurt",     50.1109, 8.6821,    "Europe/Berlin"))),
            new CountrySeed("IT", "Italy",                  "Western Europe", "EUR", List.of(
                    c("Rome",          41.9028, 12.4964,   "Europe/Rome"),
                    c("Milan",         45.4642, 9.1900,    "Europe/Rome"),
                    c("Venice",        45.4408, 12.3155,   "Europe/Rome"),
                    c("Florence",      43.7696, 11.2558,   "Europe/Rome"),
                    c("Capri",         40.5532, 14.2222,   "Europe/Rome"))),
            new CountrySeed("ES", "Spain",                  "Western Europe", "EUR", List.of(
                    c("Madrid",        40.4168, -3.7038,   "Europe/Madrid"),
                    c("Barcelona",     41.3851, 2.1734,    "Europe/Madrid"),
                    c("Mallorca",      39.6953, 3.0176,    "Europe/Madrid"),
                    c("Marbella",      36.5101, -4.8826,   "Europe/Madrid"))),
            new CountrySeed("PT", "Portugal",               "Western Europe", "EUR", List.of(
                    c("Lisbon",        38.7223, -9.1393,   "Europe/Lisbon"),
                    c("Porto",         41.1579, -8.6291,   "Europe/Lisbon"),
                    c("Madeira",       32.7607, -16.9595,  "Atlantic/Madeira"))),
            new CountrySeed("NL", "Netherlands",            "Western Europe", "EUR", List.of(
                    c("Amsterdam",     52.3676, 4.9041,    "Europe/Amsterdam"),
                    c("The Hague",     52.0705, 4.3007,    "Europe/Amsterdam"))),
            new CountrySeed("BE", "Belgium",                "Western Europe", "EUR", List.of(
                    c("Brussels",      50.8503, 4.3517,    "Europe/Brussels"),
                    c("Antwerp",       51.2194, 4.4025,    "Europe/Brussels"))),
            new CountrySeed("CH", "Switzerland",            "Western Europe", "CHF", List.of(
                    c("Zurich",        47.3769, 8.5417,    "Europe/Zurich"),
                    c("Geneva",        46.2044, 6.1432,    "Europe/Zurich"),
                    c("St. Moritz",    46.4980, 9.8333,    "Europe/Zurich"),
                    c("Zermatt",       46.0207, 7.7491,    "Europe/Zurich"))),
            new CountrySeed("AT", "Austria",                "Western Europe", "EUR", List.of(
                    c("Vienna",        48.2082, 16.3738,   "Europe/Vienna"),
                    c("Salzburg",      47.8095, 13.0550,   "Europe/Vienna"))),
            new CountrySeed("IE", "Ireland",                "Western Europe", "EUR", List.of(
                    c("Dublin",        53.3498, -6.2603,   "Europe/Dublin"),
                    c("Galway",        53.2707, -9.0568,   "Europe/Dublin"))),

            // Northern Europe
            new CountrySeed("SE", "Sweden",                 "Northern Europe","SEK", List.of(
                    c("Stockholm",     59.3293, 18.0686,   "Europe/Stockholm"),
                    c("Gothenburg",    57.7089, 11.9746,   "Europe/Stockholm"))),
            new CountrySeed("NO", "Norway",                 "Northern Europe","NOK", List.of(
                    c("Oslo",          59.9139, 10.7522,   "Europe/Oslo"),
                    c("Bergen",        60.3913, 5.3221,    "Europe/Oslo"))),
            new CountrySeed("DK", "Denmark",                "Northern Europe","DKK", List.of(
                    c("Copenhagen",    55.6761, 12.5683,   "Europe/Copenhagen"))),
            new CountrySeed("FI", "Finland",                "Northern Europe","EUR", List.of(
                    c("Helsinki",      60.1699, 24.9384,   "Europe/Helsinki"))),
            new CountrySeed("IS", "Iceland",                "Northern Europe","ISK", List.of(
                    c("Reykjavík",     64.1466, -21.9426,  "Atlantic/Reykjavik"))),

            // Southern / Eastern Europe
            new CountrySeed("GR", "Greece",                 "Southern Europe","EUR", List.of(
                    c("Athens",        37.9838, 23.7275,   "Europe/Athens"),
                    c("Mykonos",       37.4467, 25.3289,   "Europe/Athens"),
                    c("Santorini",     36.3932, 25.4615,   "Europe/Athens"))),
            new CountrySeed("HR", "Croatia",                "Southern Europe","EUR", List.of(
                    c("Dubrovnik",     42.6507, 18.0944,   "Europe/Zagreb"),
                    c("Split",         43.5081, 16.4402,   "Europe/Zagreb"))),
            new CountrySeed("PL", "Poland",                 "Eastern Europe", "PLN", List.of(
                    c("Warsaw",        52.2297, 21.0122,   "Europe/Warsaw"),
                    c("Kraków",        50.0647, 19.9450,   "Europe/Warsaw"))),
            new CountrySeed("CZ", "Czechia",                "Eastern Europe", "CZK", List.of(
                    c("Prague",        50.0755, 14.4378,   "Europe/Prague"))),
            new CountrySeed("HU", "Hungary",                "Eastern Europe", "HUF", List.of(
                    c("Budapest",      47.4979, 19.0402,   "Europe/Budapest"))),

            // East / Southeast Asia
            new CountrySeed("JP", "Japan",                  "East Asia",      "JPY", List.of(
                    c("Tokyo",         35.6762, 139.6503,  "Asia/Tokyo"),
                    c("Kyoto",         35.0116, 135.7681,  "Asia/Tokyo"),
                    c("Osaka",         34.6937, 135.5023,  "Asia/Tokyo"))),
            new CountrySeed("KR", "South Korea",            "East Asia",      "KRW", List.of(
                    c("Seoul",         37.5665, 126.9780,  "Asia/Seoul"),
                    c("Busan",         35.1796, 129.0756,  "Asia/Seoul"))),
            new CountrySeed("CN", "China",                  "East Asia",      "CNY", List.of(
                    c("Shanghai",      31.2304, 121.4737,  "Asia/Shanghai"),
                    c("Beijing",       39.9042, 116.4074,  "Asia/Shanghai"),
                    c("Chengdu",       30.5728, 104.0668,  "Asia/Shanghai"))),
            new CountrySeed("HK", "Hong Kong",              "East Asia",      "HKD", List.of(
                    c("Hong Kong",     22.3193, 114.1694,  "Asia/Hong_Kong"))),
            new CountrySeed("TW", "Taiwan",                 "East Asia",      "TWD", List.of(
                    c("Taipei",        25.0330, 121.5654,  "Asia/Taipei"))),
            new CountrySeed("SG", "Singapore",              "Southeast Asia", "SGD", List.of(
                    c("Singapore",     1.3521,  103.8198,  "Asia/Singapore"))),
            new CountrySeed("TH", "Thailand",               "Southeast Asia", "THB", List.of(
                    c("Bangkok",       13.7563, 100.5018,  "Asia/Bangkok"),
                    c("Phuket",         7.8804, 98.3923,   "Asia/Bangkok"),
                    c("Chiang Mai",    18.7883, 98.9853,   "Asia/Bangkok"))),
            new CountrySeed("MY", "Malaysia",               "Southeast Asia", "MYR", List.of(
                    c("Kuala Lumpur",  3.1390,  101.6869,  "Asia/Kuala_Lumpur"),
                    c("Penang",        5.4164,  100.3327,  "Asia/Kuala_Lumpur"))),
            new CountrySeed("ID", "Indonesia",              "Southeast Asia", "IDR", List.of(
                    c("Jakarta",       -6.2088, 106.8456,  "Asia/Jakarta"),
                    c("Bali",          -8.4095, 115.1889,  "Asia/Makassar"))),
            new CountrySeed("VN", "Vietnam",                "Southeast Asia", "VND", List.of(
                    c("Hanoi",         21.0285, 105.8542,  "Asia/Ho_Chi_Minh"),
                    c("Ho Chi Minh",   10.8231, 106.6297,  "Asia/Ho_Chi_Minh"),
                    c("Da Nang",       16.0544, 108.2022,  "Asia/Ho_Chi_Minh"))),
            new CountrySeed("PH", "Philippines",            "Southeast Asia", "PHP", List.of(
                    c("Manila",        14.5995, 120.9842,  "Asia/Manila"),
                    c("Cebu",          10.3157, 123.8854,  "Asia/Manila"))),

            // South Asia
            new CountrySeed("IN", "India",                  "South Asia",     "INR", List.of(
                    c("Mumbai",        19.0760, 72.8777,   "Asia/Kolkata"),
                    c("Delhi",         28.7041, 77.1025,   "Asia/Kolkata"),
                    c("Bangalore",     12.9716, 77.5946,   "Asia/Kolkata"),
                    c("Goa",           15.2993, 74.1240,   "Asia/Kolkata"),
                    c("Jaipur",        26.9124, 75.7873,   "Asia/Kolkata"))),
            new CountrySeed("LK", "Sri Lanka",              "South Asia",     "LKR", List.of(
                    c("Colombo",       6.9271,  79.8612,   "Asia/Colombo"),
                    c("Galle",         6.0535,  80.2210,   "Asia/Colombo"))),

            // Middle East
            new CountrySeed("AE", "United Arab Emirates",   "Middle East",    "AED", List.of(
                    c("Dubai",         25.2048, 55.2708,   "Asia/Dubai"),
                    c("Abu Dhabi",     24.4539, 54.3773,   "Asia/Dubai"))),
            new CountrySeed("SA", "Saudi Arabia",           "Middle East",    "SAR", List.of(
                    c("Riyadh",        24.7136, 46.6753,   "Asia/Riyadh"),
                    c("Jeddah",        21.4858, 39.1925,   "Asia/Riyadh"))),
            new CountrySeed("QA", "Qatar",                  "Middle East",    "QAR", List.of(
                    c("Doha",          25.2854, 51.5310,   "Asia/Qatar"))),
            new CountrySeed("OM", "Oman",                   "Middle East",    "OMR", List.of(
                    c("Muscat",        23.5880, 58.3829,   "Asia/Muscat"))),
            new CountrySeed("IL", "Israel",                 "Middle East",    "ILS", List.of(
                    c("Tel Aviv",      32.0853, 34.7818,   "Asia/Jerusalem"),
                    c("Jerusalem",     31.7683, 35.2137,   "Asia/Jerusalem"))),
            new CountrySeed("JO", "Jordan",                 "Middle East",    "JOD", List.of(
                    c("Amman",         31.9539, 35.9106,   "Asia/Amman"))),

            // Africa
            new CountrySeed("EG", "Egypt",                  "Africa",         "EGP", List.of(
                    c("Cairo",         30.0444, 31.2357,   "Africa/Cairo"))),
            new CountrySeed("MA", "Morocco",                "Africa",         "MAD", List.of(
                    c("Marrakech",     31.6295, -7.9811,   "Africa/Casablanca"),
                    c("Casablanca",    33.5731, -7.5898,   "Africa/Casablanca"))),
            new CountrySeed("ZA", "South Africa",           "Africa",         "ZAR", List.of(
                    c("Cape Town",    -33.9249, 18.4241,   "Africa/Johannesburg"),
                    c("Johannesburg", -26.2041, 28.0473,   "Africa/Johannesburg"))),
            new CountrySeed("KE", "Kenya",                  "Africa",         "KES", List.of(
                    c("Nairobi",      -1.2921,  36.8219,   "Africa/Nairobi"))),

            // South America
            new CountrySeed("BR", "Brazil",                 "South America",  "BRL", List.of(
                    c("Rio de Janeiro", -22.9068, -43.1729, "America/Sao_Paulo"),
                    c("São Paulo",      -23.5505, -46.6333, "America/Sao_Paulo"))),
            new CountrySeed("AR", "Argentina",              "South America",  "ARS", List.of(
                    c("Buenos Aires",  -34.6037, -58.3816, "America/Argentina/Buenos_Aires"))),
            new CountrySeed("CL", "Chile",                  "South America",  "CLP", List.of(
                    c("Santiago",      -33.4489, -70.6693, "America/Santiago"))),
            new CountrySeed("PE", "Peru",                   "South America",  "PEN", List.of(
                    c("Lima",          -12.0464, -77.0428, "America/Lima"))),

            // Oceania
            new CountrySeed("AU", "Australia",              "Oceania",        "AUD", List.of(
                    c("Sydney",        -33.8688, 151.2093, "Australia/Sydney"),
                    c("Melbourne",     -37.8136, 144.9631, "Australia/Melbourne"),
                    c("Perth",         -31.9505, 115.8605, "Australia/Perth"))),
            new CountrySeed("NZ", "New Zealand",            "Oceania",        "NZD", List.of(
                    c("Auckland",      -36.8485, 174.7633, "Pacific/Auckland"),
                    c("Queenstown",    -45.0312, 168.6626, "Pacific/Auckland")))
    );

    /** Common amenities shared by all generated hotels. */
    private static Amenity am(String id, String code, String name, String cat, String desc, boolean premium) {
        return new Amenity(id, code, name, cat, desc, null, premium, null, null);
    }
    private static final Amenity A_POOL  = am("am-pool",  "POOL",  "Pool",         "POOL",          "Pool",         false);
    private static final Amenity A_SPA   = am("am-spa",   "SPA",   "Spa",          "SPA",           "Spa",          true);
    private static final Amenity A_GYM   = am("am-gym",   "GYM",   "Fitness",      "FITNESS",       "Gym",          false);
    private static final Amenity A_WIFI  = am("am-wifi",  "WIFI",  "WiFi",         "CONNECTIVITY",  "WiFi",         false);
    private static final Amenity A_VALET = am("am-valet", "VALET", "Valet",        "PARKING",       "Valet",        true);
    private static final Amenity A_CONC  = am("am-conc",  "CONC",  "Concierge",    "BUSINESS",      "Concierge",    false);
    private static final Amenity A_REST  = am("am-rest",  "REST",  "Restaurant",   "DINING",        "Restaurant",   false);
    private static final Amenity A_BUS   = am("am-bus",   "BUS",   "Business",     "BUSINESS",      "Business",     false);
    private static final Amenity A_KIDS  = am("am-kids",  "KIDS",  "Kids Club",    "RECREATION",    "Kids Club",    false);

    /**
     * Generate brand records and templated hotels for them. The "Luxe Collection"
     * brand and its 5 hand-curated hotels are produced separately by the caller.
     */
    static Out generate(OffsetDateTime now,
                         Consumer<RoomType> roomSink) {
        List<Brand> brands = new ArrayList<>();
        List<Hotel> hotels = new ArrayList<>();
        List<RoomType> rooms = new ArrayList<>();
        Map<String, Integer> propertyCounts = new HashMap<>();

        // Generate hotels first (we need them to compute the per-brand property counts).
        for (BrandSpec b : BRANDS) {
            String brandId = "brand-" + b.code().toLowerCase() + "-001";
            int countriesForBrand = countriesForBrand(b);
            int countryCursor = 0;
            for (CountrySeed country : COUNTRIES) {
                if (countryCursor++ >= countriesForBrand) break;
                int hotelsHere = hotelsPerCountry(b, country);
                int picked = 0;
                for (int cityIdx = 0; cityIdx < country.cities().size() && picked < hotelsHere; cityIdx++) {
                    CitySeed city = country.cities().get(cityIdx);
                    String suffix = nameSuffix(b, country, picked);
                    Hotel h = buildHotel(b, brandId, country, city, picked, suffix, now);
                    hotels.add(h);
                    rooms.addAll(h.getRoomTypes());
                    h.getRoomTypes().forEach(roomSink);
                    propertyCounts.merge(brandId, 1, Integer::sum);
                    picked++;
                }
            }
        }

        // Now build brand records with the actual property counts.
        for (BrandSpec b : BRANDS) {
            String brandId = "brand-" + b.code().toLowerCase() + "-001";
            int count = propertyCounts.getOrDefault(brandId, 0);
            String logoUrl = "https://cdn.luxe.com/brands/" + b.slug() + "-logo.svg";
            String heroUrl = "https://cdn.luxe.com/brands/" + b.slug() + "-hero.jpg";
            brands.add(new Brand(brandId, b.code(), b.name(), b.slug(), b.tier(),
                    b.tagline(),
                    "The " + b.name() + " collection — " + b.tagline().toLowerCase(),
                    logoUrl, heroUrl, b.accent(),
                    b.pointsMultiplier(), count,
                    "Carbon-conscious operations across all properties.",
                    now.minusYears(8), now));
        }

        // Sort by tier then name for predictable order.
        brands.sort(Comparator.comparing(Brand::getTier).reversed().thenComparing(Brand::getName));
        return new Out(brands, hotels, rooms);
    }

    /** How many countries a brand spans — luxury/premium fan out wider than select. */
    private static int countriesForBrand(BrandSpec b) {
        return switch (b.tier()) {
            case "LUXURY"  -> COUNTRIES.size();          // ~50
            case "PREMIUM" -> Math.min(COUNTRIES.size(), 38);
            case "SELECT"  -> Math.min(COUNTRIES.size(), 28);
            default        -> 20;
        };
    }

    /** Hotels per country for a given brand: 1–4, deterministic per (brand, country). */
    private static int hotelsPerCountry(BrandSpec b, CountrySeed country) {
        int seed = Math.abs((b.code() + "::" + country.code()).hashCode());
        // Bias toward 2–3, with occasional 4 for big-market countries.
        int base = 2 + (seed % 3); // 2, 3, or 4
        if (country.cities().size() == 1) return Math.min(base, 1);
        return Math.min(base, country.cities().size());
    }

    private static String nameSuffix(BrandSpec b, CountrySeed country, int idx) {
        if (idx == 0) return "";
        String[] suffixes = { "Downtown", "Marina", "Heritage", "Riverside", "Park", "Old Town", "Bay" };
        int seed = Math.abs((b.code() + country.code() + idx).hashCode());
        return " " + suffixes[seed % suffixes.length];
    }

    private static Hotel buildHotel(BrandSpec b, String brandId, CountrySeed country, CitySeed city,
                                     int idx, String suffix, OffsetDateTime now) {
        String hid = "prop-" + b.code().toLowerCase() + "-" +
                country.code().toLowerCase() + "-" +
                slugify(city.name()) + (idx > 0 ? "-" + (idx + 1) : "");
        String displayName = b.name() + " " + city.name() + suffix;
        String slug = slugify(displayName);
        int seed = Math.abs(hid.hashCode());

        int starRating = switch (b.tier()) {
            case "LUXURY"  -> 5;
            case "PREMIUM" -> 4 + (seed % 2);  // 4 or 5
            case "SELECT"  -> 3 + (seed % 2);  // 3 or 4
            default        -> 4;
        };
        double overall = switch (b.tier()) {
            case "LUXURY"  -> 8.8 + (seed % 9) * 0.1;   // 8.8 – 9.6
            case "PREMIUM" -> 8.4 + (seed % 9) * 0.1;   // 8.4 – 9.2
            case "SELECT"  -> 7.8 + (seed % 11) * 0.1;  // 7.8 – 8.8
            default        -> 8.0;
        };
        int reviewCount = 200 + (seed % 1800);
        int totalRooms = switch (b.tier()) {
            case "LUXURY"  -> 120 + (seed % 250);
            case "PREMIUM" -> 180 + (seed % 320);
            case "SELECT"  -> 80  + (seed % 280);
            default        -> 150;
        };
        int floors = 4 + (seed % 32);
        int openedYear = 1985 + (seed % 38);
        Integer renovatedYear = (seed % 3 == 0) ? openedYear + 8 + (seed % 6) : null;

        // Slight jitter on coordinates so multiple hotels per city aren't co-located.
        double latJitter = ((seed >> 4) % 100) / 1000.0 - 0.05;
        double lngJitter = ((seed >> 8) % 100) / 1000.0 - 0.05;

        HotelLocation loc = new HotelLocation(
                new Address(addressLine(b, city, idx), null, city.name(), null, postalCode(country, seed),
                        country.code(), country.name()),
                new Coordinates(round(city.lat() + latJitter, 4), round(city.lng() + lngJitter, 4)),
                city.tz(),
                null,
                null);

        HotelContact contact = new HotelContact(
                "+00-" + (1000 + seed % 9000) + "-" + (10000 + (seed * 7) % 89999),
                slugify(b.name()) + "-" + slugify(city.name()) + "@luxehotels.com",
                "https://" + slugify(b.name()) + ".luxehotels.com/" + slugify(city.name()),
                null, null);

        HotelPolicies policies = new HotelPolicies(
                new HotelPolicies.CheckInPolicy("15:00", new HotelPolicies.EarlyLatePolicy(true, country.currency() + " 50", true)),
                new HotelPolicies.CheckOutPolicy("12:00", new HotelPolicies.EarlyLatePolicy(true, country.currency() + " 50", true)),
                new HotelPolicies.CancellationPolicy(48, "Free cancellation up to 48h before arrival."),
                new HotelPolicies.PetPolicy(b.tier().equals("SELECT") || (seed % 4 == 0), null,
                        b.tier().equals("SELECT") ? "Pets welcome on request." : "Small pets welcome (under 10kg)."),
                "Non-smoking throughout.",
                "Children of all ages welcome.");

        List<Amenity> amenities = amenitiesForTier(b.tier(), seed);
        boolean hasSpa = amenities.contains(A_SPA);
        boolean hasPool = amenities.contains(A_POOL);

        Spa spa = hasSpa ? new Spa("spa-" + hid, hid, b.name() + " Spa " + city.name(),
                LocalizedContent.of("A signature spa experience by " + b.name() + "."),
                "07:00–22:00", true) : null;

        List<Restaurant> restaurants = (amenities.contains(A_REST) || b.tier().equals("LUXURY"))
                ? List.of(new Restaurant("rest-" + hid, hid, "The " + city.name() + " Table",
                        List.of("Seasonal", b.tier().equals("LUXURY") ? "Fine dining" : "All-day dining"),
                        LocalizedContent.of("Signature in-house dining at " + displayName + "."),
                        b.tier().equals("LUXURY") ? "Smart" : "Casual",
                        "06:30–22:30",
                        country.currency() + " " + (b.tier().equals("LUXURY") ? "$$$$" : b.tier().equals("PREMIUM") ? "$$$" : "$$"),
                        b.tier().equals("LUXURY"), true))
                : List.of();

        List<MediaAsset> media = List.of(
                new MediaAsset("m-" + hid + "-1", "https://cdn.luxe.com/" + b.slug() + "/" + slugify(city.name()) + "/exterior.jpg",
                        "https://cdn.luxe.com/" + b.slug() + "/" + slugify(city.name()) + "/exterior_thumb.jpg",
                        "EXTERIOR", "Exterior view", null, true, 1),
                new MediaAsset("m-" + hid + "-2", "https://cdn.luxe.com/" + b.slug() + "/" + slugify(city.name()) + "/lobby.jpg",
                        "https://cdn.luxe.com/" + b.slug() + "/" + slugify(city.name()) + "/lobby_thumb.jpg",
                        "INTERIOR", "Lobby", null, false, 2)
        );

        SustainabilityInfo sustainability = new SustainabilityInfo(
                60 + (seed % 35),
                List.of(),
                seed % 2 == 0,
                40 + (seed % 50),
                30 + (seed % 60));

        boolean isFeatured = "LUXURY".equals(b.tier()) && idx == 0 && (seed % 7 == 0);

        Hotel h = new Hotel(hid, brandId, b.code(),
                country.code() + String.format("%03d", 1 + (seed % 999)),
                displayName, slug, "ACTIVE",
                openedYear, renovatedYear, floors, totalRooms, starRating,
                new GuestRating(round(overall, 1), reviewCount,
                        new RatingBreakdown(reviewCount * 7 / 10, reviewCount * 2 / 10,
                                reviewCount / 20, reviewCount / 50, reviewCount / 100),
                        List.of()),
                loc, contact,
                LocalizedContent.of(displayName + " brings the " + b.name() + " philosophy to " + city.name() +
                        ". " + b.tagline()),
                policies,
                amenities,
                new ArrayList<>(),  // room types attached below
                restaurants, spa, null,
                media,
                List.of(),
                new ParkingInfo(true, "Multi-storey", b.tier().equals("LUXURY"),
                        country.currency() + " 30/day", null, true),
                sustainability,
                List.of(),
                isFeatured, now);

        // Add a tiered set of room types.
        List<RoomType> rt = roomTypesFor(h, b, city, seed);
        h.getRoomTypes().addAll(rt);
        return h;
    }

    private static List<Amenity> amenitiesForTier(String tier, int seed) {
        List<Amenity> base = new ArrayList<>(List.of(A_WIFI, A_GYM, A_REST));
        switch (tier) {
            case "LUXURY" -> { base.addAll(List.of(A_POOL, A_SPA, A_VALET, A_CONC, A_BUS)); }
            case "PREMIUM" -> {
                base.add(A_CONC); base.add(A_BUS);
                if (seed % 3 != 0) base.add(A_POOL);
                if (seed % 2 == 0) base.add(A_SPA);
                if (seed % 4 == 0) base.add(A_KIDS);
            }
            case "SELECT" -> {
                if (seed % 4 == 0) base.add(A_POOL);
                if (seed % 5 == 0) base.add(A_BUS);
            }
        }
        return base;
    }

    private static List<RoomType> roomTypesFor(Hotel h, BrandSpec b, CitySeed city, int seed) {
        List<RoomType> out = new ArrayList<>();
        // Always have a Deluxe and a Suite. LUXURY gets a Penthouse too.
        out.add(room(h.getId() + "-rm1", h.getId(), "DLX",
                "Deluxe " + (b.tier().equals("LUXURY") ? "King" : "Room"),
                "DELUXE", "Refined room with city outlook.", 32 + (seed % 18),
                new OccupancyLimit(2, 1, 3),
                List.of(new BedConfiguration("KING", 1)),
                "City view", "3-12",
                List.of("Premium linens", "Smart room controls"),
                false, false));
        out.add(room(h.getId() + "-rm2", h.getId(), "EXE",
                "Executive Room",
                "PREMIER", "Elevated room with extra space and lounge access.", 40 + (seed % 14),
                new OccupancyLimit(2, 1, 3),
                List.of(new BedConfiguration("KING", 1)),
                "Skyline view", "10-20",
                List.of("Lounge access", "Welcome amenities"),
                false, false));
        out.add(room(h.getId() + "-rm3", h.getId(), "STE",
                b.tier().equals("LUXURY") ? "Signature Suite" : "Junior Suite",
                "SUITE", "Suite with separate living area.", 60 + (seed % 22),
                new OccupancyLimit(3, 2, 5),
                List.of(new BedConfiguration("KING", 1), new BedConfiguration("SOFA_BED", 1)),
                "Premium view", "12-25",
                List.of("Living area", "Marble bathroom"),
                false, true));
        if ("LUXURY".equals(b.tier())) {
            out.add(room(h.getId() + "-rm4", h.getId(), "PNT",
                    "Penthouse",
                    "PENTHOUSE", "Top-floor penthouse with private terrace.", 140.0 + (seed % 60),
                    new OccupancyLimit(4, 2, 6),
                    List.of(new BedConfiguration("KING", 2)),
                    "Panoramic", "Top",
                    List.of("Private terrace", "Butler service"),
                    false, false));
        }
        return out;
    }

    private static RoomType room(String id, String hotelId, String code, String name, String category,
                                  String desc, double sqm, OccupancyLimit occ, List<BedConfiguration> beds,
                                  String view, String floor, List<String> highlights, boolean smoking, boolean connecting) {
        return new RoomType(id, hotelId, code, name, category,
                LocalizedContent.of(desc), sqm, occ, beds, view, floor,
                List.of(),
                List.of(new MediaAsset(id + "-img",
                        "https://cdn.luxe.com/rooms/" + id + ".jpg",
                        "https://cdn.luxe.com/rooms/" + id + "_thumb.jpg",
                        "ROOM", null, null, true, 1)),
                List.of(),
                highlights, smoking, connecting);
    }

    private static String addressLine(BrandSpec b, CitySeed city, int idx) {
        int n = 1 + Math.abs((b.code() + city.name() + idx).hashCode()) % 240;
        String[] streets = { "High Street", "Park Avenue", "Marina Boulevard", "Central Square",
                "Riverside Drive", "Heritage Lane", "Old Town Way", "Garden Crescent" };
        return n + " " + streets[Math.abs((b.code() + city.name()).hashCode()) % streets.length];
    }

    private static String postalCode(CountrySeed country, int seed) {
        int code = 10000 + Math.abs(seed) % 89999;
        return switch (country.code()) {
            case "GB" -> "SW1A 1A" + (Math.abs(seed) % 9);
            case "US", "JP", "DE", "FR", "ES" -> String.valueOf(code);
            default -> String.valueOf(code);
        };
    }

    private static double round(double d, int decimals) {
        double k = Math.pow(10, decimals);
        return Math.round(d * k) / k;
    }

    private static String slugify(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
