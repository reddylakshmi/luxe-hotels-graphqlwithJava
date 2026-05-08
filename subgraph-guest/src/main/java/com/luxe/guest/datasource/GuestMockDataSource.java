package com.luxe.guest.datasource;

import com.luxe.guest.schema.types.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GuestMockDataSource implements GuestDataSource {

    private final Map<String, GuestProfile> guests = new LinkedHashMap<>();

    public GuestMockDataSource() {
        initData();
    }

    private void initData() {
        add(profile("guest-001", "sophia.chen@email.com", "+1-415-555-0101",
                new GuestName("Ms", "Sophia", null, "Chen", null, null),
                new GuestExternalIds("LUX0001234567", null, null, null),
                "Chinese", "en", "USD",
                prefs("City view", "High floor", "King", "Soft",
                        List.of("Vegetarian"), List.of(), false, false, false, "en"),
                List.of(
                        address("HOME", "123 Powell St", "San Francisco", "CA", "94102", "US", true),
                        address("WORK", "1 Market St", "San Francisco", "CA", "94105", "US", false)
                ),
                List.of(
                        pm("pm-001", "CREDIT_CARD", "Visa", "4242", "Sophia Chen", "tok_001",
                                12, 2026, null, true),
                        pm("pm-002", "CREDIT_CARD", "Amex", "1234", "Sophia Chen", "tok_002",
                                6, 2027, null, false)
                ),
                List.of(
                        companion("tc-001", new GuestName("Mr", "David", null, "Chen", null, null),
                                "spouse", "david.chen@email.com", "+1-415-555-0102", null, LocalDate.of(1983, 7, 20))
                ),
                List.of(
                        new SavedHotel("sh-001", "prop-paris-001", OffsetDateTime.now().minusDays(30)),
                        new SavedHotel("sh-002", "prop-tokyo-001", OffsetDateTime.now().minusDays(15))
                ),
                null,
                LocalDate.of(1985, 3, 15), "https://cdn.luxehotels.com/avatars/guest-001.jpg",
                OffsetDateTime.now().minusYears(3), OffsetDateTime.now().minusDays(10)));

        add(profile("guest-002", "james.morrison@email.com", "+44-20-7946-0958",
                new GuestName("Mr", "James", null, "Morrison", null, null),
                new GuestExternalIds("LUX0002345678", null, null, null),
                "British", "en", "GBP",
                prefs(null, "Low floor", "King", "Firm",
                        List.of(), List.of(), false, false, false, "en"),
                List.of(address("HOME", "14 Belgravia Sq", "London", null, "SW1X 8PZ", "GB", true)),
                List.of(pm("pm-003", "CREDIT_CARD", "Mastercard", "5678", "James Morrison", "tok_003",
                        3, 2025, null, true)),
                List.of(
                        companion("tc-002", new GuestName("Mrs", "Sarah", null, "Morrison", null, null),
                                "spouse", "sarah.morrison@email.com", "+44-20-7946-0959", null, LocalDate.of(1980, 4, 12)),
                        companion("tc-003", new GuestName(null, "Oliver", null, "Morrison", null, null),
                                "child", null, null, null, LocalDate.of(2010, 8, 5))
                ),
                List.of(
                        new SavedHotel("sh-003", "prop-london-001", OffsetDateTime.now().minusDays(60))
                ),
                null,
                LocalDate.of(1978, 11, 22), null,
                OffsetDateTime.now().minusYears(5), OffsetDateTime.now().minusDays(3)));

        add(profile("guest-003", "yuki.tanaka@email.com", "+81-3-5555-0123",
                new GuestName("Ms", "Yuki", null, "Tanaka", null, null),
                new GuestExternalIds(null, null, null, null),
                "Japanese", "ja", "JPY",
                prefs(null, null, "Twin", "Medium",
                        List.of("No shellfish"), List.of("Shellfish"), false, false, false, "ja"),
                List.of(address("HOME", "2-3-4 Shibuya", "Tokyo", null, "150-0002", "JP", true)),
                List.of(pm("pm-004", "CREDIT_CARD", "JCB", "9012", "Yuki Tanaka", "tok_004",
                        9, 2026, null, true)),
                List.of(),
                List.of(new SavedHotel("sh-004", "prop-tokyo-001", OffsetDateTime.now().minusDays(5))),
                null,
                LocalDate.of(1990, 7, 8), null,
                OffsetDateTime.now().minusMonths(8), OffsetDateTime.now().minusDays(1)));

        add(profile("guest-004", "fatima.al-rashid@email.com", "+971-50-555-0199",
                new GuestName("Ms", "Fatima", null, "Al-Rashid", null, null),
                new GuestExternalIds("LUX0003456789", null, null, null),
                "Emirati", "ar", "AED",
                prefs("Sea view", "High floor", "King", "Soft",
                        List.of(), List.of("Pork"), false, true, false, "ar"),
                List.of(address("HOME", "Palm Jumeirah, Villa 42", "Dubai", null, "00000", "AE", true)),
                List.of(
                        pm("pm-005", "CREDIT_CARD", "Visa", "3456", "Fatima Al-Rashid", "tok_005",
                                11, 2027, null, true)
                ),
                List.of(),
                List.of(
                        new SavedHotel("sh-005", "prop-dubai-001", OffsetDateTime.now().minusDays(20)),
                        new SavedHotel("sh-006", "prop-paris-001", OffsetDateTime.now().minusDays(45))
                ),
                new GuestCorporateProfile("Emirates Group", "CORP-EG-001", "FAR-8821", "CC-VIP"),
                LocalDate.of(1982, 4, 30), null,
                OffsetDateTime.now().minusYears(2), OffsetDateTime.now().minusDays(5)));

        add(profile("guest-005", "marco.ferrari@email.com", "+39-02-5555-0188",
                new GuestName("Mr", "Marco", null, "Ferrari", null, null),
                new GuestExternalIds("LUX0004567890", null, null, null),
                "Italian", "it", "EUR",
                prefs(null, "Mid floor", "King", "Soft",
                        List.of("Gluten-free"), List.of("Gluten"), false, false, false, "it"),
                List.of(address("HOME", "Via della Spiga 42", "Milan", null, "20121", "IT", true)),
                List.of(pm("pm-006", "CREDIT_CARD", "Visa", "7890", "Marco Ferrari", "tok_006",
                        5, 2026, null, true)),
                List.of(companion("tc-004", new GuestName("Ms", "Lucia", null, "Ferrari", null, null),
                        "spouse", "lucia.ferrari@email.com", "+39-02-5555-0189", null, LocalDate.of(1977, 6, 14))),
                List.of(
                        new SavedHotel("sh-007", "prop-paris-001", OffsetDateTime.now().minusDays(90)),
                        new SavedHotel("sh-008", "prop-dubai-001", OffsetDateTime.now().minusDays(30))
                ),
                new GuestCorporateProfile("Ferrari Luxury Brands", "CORP-FLB-001", "MF-001", "EXEC"),
                LocalDate.of(1975, 9, 12), null,
                OffsetDateTime.now().minusYears(7), OffsetDateTime.now().minusDays(2)));

        add(profile("guest-006", "diana.okonkwo@email.com", "+234-81-5550-1234",
                new GuestName("Dr", "Diana", null, "Okonkwo", null, null),
                new GuestExternalIds(null, null, null, null),
                "Nigerian", "en", "USD",
                prefs(null, "High floor", "King", null,
                        List.of(), List.of(), false, false, false, "en"),
                List.of(address("HOME", "3 Bourdillon Rd", "Lagos", null, "101233", "NG", true)),
                List.of(pm("pm-007", "CREDIT_CARD", "Mastercard", "2468", "Diana Okonkwo", "tok_007",
                        8, 2028, null, true)),
                List.of(),
                List.of(),
                null,
                LocalDate.of(1993, 1, 25), null,
                OffsetDateTime.now().minusMonths(4), OffsetDateTime.now().minusDays(15)));
    }

    private GuestProfile profile(String id, String email, String phone,
                                  GuestName name, GuestExternalIds externalIds,
                                  String nationality, String lang, String currency,
                                  GuestPreferences prefs,
                                  List<GuestAddress> addresses, List<PaymentMethod> paymentMethods,
                                  List<TravelCompanion> companions, List<SavedHotel> savedHotels,
                                  GuestCorporateProfile corporate,
                                  LocalDate dob, String avatarUrl,
                                  OffsetDateTime memberSince, OffsetDateTime updatedAt) {
        return new GuestProfile(id, email, phone, nationality, avatarUrl,
                lang, currency, dob, name, externalIds, prefs,
                new ArrayList<>(addresses),
                new ArrayList<>(paymentMethods),
                new ArrayList<>(companions),
                new ArrayList<>(savedHotels),
                corporate, memberSince, updatedAt);
    }

    private GuestPreferences prefs(String view, String floor, String bedType, String pillow,
                                    List<String> restrictions, List<String> allergies,
                                    Boolean kosher, Boolean halal, Boolean vegan, String lang) {
        return new GuestPreferences(
                new GuestPreferences.RoomPreferences(List.of(), view, floor, false, false, false, false),
                new GuestPreferences.BedPreferences(bedType, null),
                new GuestPreferences.PillowPreferences(pillow, null),
                new GuestPreferences.DietaryPreferences(restrictions, allergies, kosher, halal, vegan),
                new GuestPreferences.AccessibilityNeeds(false, false, false),
                new GuestPreferences.TransportPreferences(null, List.of()),
                new GuestPreferences.CommunicationPreferences(lang, "EMAIL", true, false)
        );
    }

    private GuestAddress address(String type, String line1, String city, String state,
                                  String postal, String country, boolean isPrimary) {
        String id = "addr-" + type + "-" + UUID.randomUUID().toString().substring(0, 6);
        return new GuestAddress(id, type, line1, null, city, state, postal, country, isPrimary);
    }

    private PaymentMethod pm(String id, String type, String brand, String lastFour,
                              String holder, String token, int month, int year,
                              GuestAddress billing, boolean isDefault) {
        return new PaymentMethod(id, type, brand, lastFour, holder, token, month, year,
                billing, isDefault, OffsetDateTime.now().minusMonths(6));
    }

    private TravelCompanion companion(String id, GuestName name, String relationship,
                                       String email, String phone, String loyaltyNumber,
                                       LocalDate dob) {
        return new TravelCompanion(id, name, relationship, email, phone, loyaltyNumber,
                dob, OffsetDateTime.now().minusMonths(3));
    }

    private void add(GuestProfile g) { guests.put(g.getId(), g); }

    @Override
    public Optional<GuestProfile> findById(String id) {
        return Optional.ofNullable(guests.get(id));
    }

    @Override
    public Optional<GuestProfile> findByEmail(String email) {
        return guests.values().stream()
                .filter(g -> g.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public List<GuestProfile> findAll() { return new ArrayList<>(guests.values()); }

    @Override
    public List<GuestProfile> findByFilter(String email, String status, String loyaltyNumber) {
        return guests.values().stream()
                .filter(g -> email == null || g.getEmail().contains(email))
                .filter(g -> loyaltyNumber == null ||
                        (g.getExternalIds() != null && loyaltyNumber.equals(g.getExternalIds().loyaltyNumber())))
                .collect(Collectors.toList());
    }

    @Override
    public AuthPayload signIn(String email, String password) {
        GuestProfile guest = findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        String token = "mock-jwt-" + guest.getId() + "-GUEST";
        return new AuthPayload(token, "mock-refresh-" + guest.getId(), 3600, "Bearer", guest, false);
    }

    @Override
    public AuthPayload signUp(String email, String password, String firstName, String lastName, String phone) {
        if (existsByEmail(email)) throw new RuntimeException("Email already registered");
        String id = "guest-" + UUID.randomUUID().toString().substring(0, 8);
        GuestProfile g = new GuestProfile(id, email, phone, null, null, "en", "USD",
                null, new GuestName(null, firstName, null, lastName, null, null),
                new GuestExternalIds(null, null, null, null),
                GuestPreferences.defaults(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                null, OffsetDateTime.now(), OffsetDateTime.now());
        guests.put(id, g);
        String token = "mock-jwt-" + id + "-GUEST";
        return new AuthPayload(token, "mock-refresh-" + id, 3600, "Bearer", g, true);
    }

    @Override
    public GuestProfile update(String id, Map<String, Object> fields) {
        return guests.get(id);
    }

    @Override
    public GuestProfile updatePreferences(String id, GuestPreferences preferences) {
        GuestProfile g = guests.get(id);
        if (g == null) return null;
        g.setPreferences(preferences);
        return g;
    }

    @Override
    public GuestProfile addPaymentMethod(String guestId, Map<String, Object> input) {
        GuestProfile g = guests.get(guestId);
        if (g == null) return null;
        String id = "pm-" + UUID.randomUUID().toString().substring(0, 8);
        boolean isDefault = Boolean.TRUE.equals(input.get("setAsDefault"));
        if (isDefault) g.clearDefaultPaymentMethods();
        PaymentMethod pm = new PaymentMethod(id,
                (String) input.getOrDefault("type", "CREDIT_CARD"),
                (String) input.getOrDefault("brand", "Unknown"),
                (String) input.getOrDefault("lastFour", "0000"),
                (String) input.getOrDefault("holderName", ""),
                (String) input.getOrDefault("pspToken", "tok_" + id),
                ((Number) input.getOrDefault("expiryMonth", 12)).intValue(),
                ((Number) input.getOrDefault("expiryYear", 2030)).intValue(),
                null, isDefault, OffsetDateTime.now());
        g.addPaymentMethodToList(pm);
        return g;
    }

    @Override
    public GuestProfile removePaymentMethod(String guestId, String paymentMethodId) {
        GuestProfile g = guests.get(guestId);
        if (g == null) return null;
        g.removePaymentMethodFromList(paymentMethodId);
        return g;
    }

    @Override
    public GuestProfile setDefaultPaymentMethod(String guestId, String paymentMethodId) {
        GuestProfile g = guests.get(guestId);
        if (g == null) return null;
        g.clearDefaultPaymentMethods();
        g.getPaymentMethods().stream()
                .filter(pm -> pm.getId().equals(paymentMethodId))
                .findFirst()
                .ifPresent(pm -> pm.setDefault(true));
        return g;
    }

    @Override
    public GuestProfile saveHotel(String guestId, String hotelId) {
        GuestProfile g = guests.get(guestId);
        if (g == null) return null;
        boolean alreadySaved = g.getSavedHotels().stream().anyMatch(h -> h.hotelId().equals(hotelId));
        if (!alreadySaved) {
            String id = "sh-" + UUID.randomUUID().toString().substring(0, 8);
            g.addSavedHotelToList(new SavedHotel(id, hotelId, OffsetDateTime.now()));
        }
        return g;
    }

    @Override
    public GuestProfile unsaveHotel(String guestId, String hotelId) {
        GuestProfile g = guests.get(guestId);
        if (g == null) return null;
        g.removeSavedHotelFromList(hotelId);
        return g;
    }

    @Override
    public GuestProfile addTravelCompanion(String guestId, Map<String, Object> input) {
        GuestProfile g = guests.get(guestId);
        if (g == null) return null;
        String id = "tc-" + UUID.randomUUID().toString().substring(0, 8);
        @SuppressWarnings("unchecked")
        Map<String, Object> nameInput = (Map<String, Object>) input.get("name");
        GuestName name = nameInput != null
                ? new GuestName(
                        (String) nameInput.get("title"),
                        (String) nameInput.get("firstName"),
                        (String) nameInput.get("middleName"),
                        (String) nameInput.get("lastName"),
                        (String) nameInput.get("preferredName"),
                        (String) nameInput.get("suffix"))
                : new GuestName(null, "Unknown", null, "", null, null);
        String dobStr = (String) input.get("dateOfBirth");
        LocalDate dob = dobStr != null ? LocalDate.parse(dobStr) : null;
        TravelCompanion tc = new TravelCompanion(id, name, (String) input.get("relationship"),
                (String) input.get("email"), (String) input.get("phone"),
                (String) input.get("loyaltyNumber"), dob, OffsetDateTime.now());
        g.addTravelCompanionToList(tc);
        return g;
    }

    @Override
    public GuestProfile removeTravelCompanion(String guestId, String companionId) {
        GuestProfile g = guests.get(guestId);
        if (g == null) return null;
        g.removeTravelCompanionFromList(companionId);
        return g;
    }

    @Override
    public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }
}
