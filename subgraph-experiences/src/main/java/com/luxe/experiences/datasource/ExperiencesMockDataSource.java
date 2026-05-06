package com.luxe.experiences.datasource;

import com.luxe.common.scalar.Money;
import com.luxe.experiences.schema.types.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ExperiencesMockDataSource implements ExperiencesDataSource {

    private final Map<String, Experience> experiences = new LinkedHashMap<>();
    private final Map<String, SpaTreatment> spaTreatments = new LinkedHashMap<>();
    private final Map<String, ExperienceBooking> bookings = new LinkedHashMap<>();
    private final Set<String> validSlotTokens = new HashSet<>();

    private final Map<String, String> restaurantHotel = new HashMap<>();
    private final Map<String, String> restaurantName  = new HashMap<>();
    private final Map<String, String> restaurantCuisine = new HashMap<>();
    private final Map<String, Integer> restaurantStars = new HashMap<>();

    private final Map<String, String> courseHotel = new HashMap<>();
    private final Map<String, String> courseName  = new HashMap<>();

    public ExperiencesMockDataSource() {
        initExperiences();
        initSpaTreatments();
        initRestaurants();
        initCourses();
    }

    // ── Experiences ──────────────────────────────────────────────────────────

    private void initExperiences() {
        addExperience("exp-001", "prop-paris-001",
                "Hands-on Macaron Workshop", "CULINARY",
                "Two hours of pâtisserie with a Le Grand Luxe chef",
                "Spend the morning side by side with our pâtissier learning the rhythm and rigor behind the perfect macaron — from meringue to ganache.",
                120, Money.of(180, "EUR"), "EUR", 8, 2,
                List.of("Two hours with senior pâtissier",
                        "Take home a dozen macarons",
                        "Champagne pairing"),
                List.of("All ingredients", "Branded apron", "Recipe booklet"),
                List.of("Comfortable shoes"),
                "EASY", null, true);

        addExperience("exp-002", "prop-tokyo-001",
                "Pre-dawn Tsukiji Market Tour", "CULINARY",
                "An insider's walk through the world's most famous fish market",
                "Begin at 4:30 AM at the Toyosu market with a private guide who has worked the floor for 22 years. End with a chef-led omakase breakfast.",
                180, Money.of(45000, "JPY"), "JPY", 6, 1,
                List.of("Private market guide",
                        "Omakase breakfast at Daiwa",
                        "Photo permits arranged"),
                List.of("Round-trip transport", "Translator"),
                List.of("Closed-toe shoes"),
                "MODERATE", 12, true);

        addExperience("exp-003", "prop-dubai-001",
                "Sunset Dune Photography Safari", "ADVENTURE",
                "A photographer's expedition through the Liwa dunes",
                "Travel into the deep desert with a National Geographic-trained photographer. Receive professional retouching of your favorite three frames.",
                240, Money.of(2200, "AED"), "AED", 4, 1,
                List.of("Private 4WD with driver",
                        "Professional photo guidance",
                        "Mineral water and date pairing at sunset"),
                List.of("Camera gear support", "Bottled water"),
                List.of("Sun protection", "Layers for evening"),
                "MODERATE", 14, true);

        addExperience("exp-004", "prop-london-001",
                "After-hours at the V&A", "CULTURAL",
                "Private gallery access after closing",
                "A curator-led walk through the most-loved galleries of the V&A — without the crowds.",
                90, Money.of(220, "GBP"), "GBP", 12, 4,
                List.of("Private curator", "Champagne welcome",
                        "Take-home exhibition catalogue"),
                List.of("Welcome drink", "Curated catalogue"),
                List.of(),
                "EASY", null, false);

        addExperience("exp-005", "prop-nyc-001",
                "Private Helicopter Skyline Tour", "TOURS",
                "Manhattan from above at golden hour",
                "Fly from the West 30th Street Heliport over the Statue of Liberty, the Bridges, Central Park, and the Empire State Building. 30 minutes airborne.",
                60, Money.of(950, "USD"), "USD", 5, 2,
                List.of("Direct private heliport access",
                        "Custom flight path",
                        "Noise-canceling headsets"),
                List.of("Flight insurance",
                        "Champagne toast on landing"),
                List.of(),
                "EASY", 8, true);

        addExperience("exp-006", "prop-paris-001",
                "Sound Bath in the Orangerie", "SPA_WELLNESS",
                "60-minute crystal sound bath in our private courtyard",
                "Lie back beneath century-old plane trees as our resident sound healer guides you through a Tibetan bowl session.",
                60, Money.of(160, "EUR"), "EUR", 14, 4,
                List.of("Tibetan singing bowls",
                        "Aromatherapy", "Herbal tea ritual"),
                List.of("Mat", "Eye pillow", "Tea service"),
                List.of("Comfortable clothing"),
                "EASY", null, false);
    }

    private void addExperience(String id, String hotelId, String name, String category,
                                String description, String longDescription,
                                Integer durationMinutes, Money price, String currency,
                                int max, int min, List<String> highlights, List<String> included,
                                List<String> bringYourOwn, String difficulty, Integer ageMinimum,
                                boolean featured) {
        experiences.put(id, new Experience(id, hotelId, name, category,
                description, longDescription, durationMinutes, price, currency,
                max, min, true, highlights, included, bringYourOwn,
                List.of("https://content.luxehotels.example/experiences/" + id + ".jpg"),
                featured, true, difficulty, ageMinimum));
    }

    // ── Spa Treatments ───────────────────────────────────────────────────────

    private void initSpaTreatments() {
        addSpa("spa-001", "prop-paris-001", "Signature Luxe Massage",        "SIGNATURE",
                "A 90-minute full-body massage with bespoke aromatherapy", 90,
                Money.of(280, "EUR"),
                List.of("Aromatherapy", "Hot stones", "Lymphatic drainage"),
                List.of("Pregnancy", "Acute injury"), 16);
        addSpa("spa-002", "prop-paris-001", "Provence Honey Facial",         "FACIAL",
                "Brightening facial with locally sourced lavender honey",   75,
                Money.of(220, "EUR"),
                List.of("LED therapy", "Lymphatic massage"),
                List.of("Active retinol use within 7 days"), 18);
        addSpa("spa-003", "prop-tokyo-001", "Forest Bathing Ritual",          "WELLNESS_RITUAL",
                "Two hours of guided shinrin-yoku and onsen soak",         120,
                Money.of(38000, "JPY"),
                List.of("Forest meditation", "Onsen", "Aromatherapy"),
                List.of("Cardiovascular conditions"), 14);
        addSpa("spa-004", "prop-dubai-001", "Arabian Hammam Experience",      "HYDROTHERAPY",
                "Traditional hammam ritual with rose otto and gold dust", 120,
                Money.of(750, "AED"),
                List.of("Hammam", "Body scrub", "Steam"), List.of("Pregnancy"), 16);
        addSpa("spa-005", "prop-london-001", "Mayfair Couples Retreat",       "COUPLES",
                "Side-by-side massage in our largest treatment suite",     90,
                Money.of(540, "GBP"),
                List.of("Champagne service", "Side-by-side suite"),
                List.of(), 18);
        addSpa("spa-006", "prop-nyc-001", "Manhattan Express Reset",          "MASSAGE",
                "A 50-minute energizing massage for the time-pressed traveler", 50,
                Money.of(195, "USD"),
                List.of("Trigger-point release"), List.of(), 16);
    }

    private void addSpa(String id, String hotelId, String name, String category, String desc,
                         int durMins, Money price, List<String> modalities,
                         List<String> contra, Integer ageMin) {
        spaTreatments.put(id, new SpaTreatment(id, hotelId, name, category, desc,
                durMins, price, modalities, contra, ageMin));
    }

    // ── Restaurants / golf ───────────────────────────────────────────────────

    private void initRestaurants() {
        addRestaurant("rest-paris-001", "prop-paris-001", "Le Grand Salon", "Modern French", 2);
        addRestaurant("rest-paris-002", "prop-paris-001", "Café Bastille",  "Bistro",         null);
        addRestaurant("rest-tokyo-001", "prop-tokyo-001", "Ginza Sushiko",  "Edomae sushi",   3);
        addRestaurant("rest-dubai-001", "prop-dubai-001", "Sky Terrace",    "Mediterranean",  1);
        addRestaurant("rest-london-001", "prop-london-001", "Mayfair House", "Modern British", 1);
        addRestaurant("rest-nyc-001",   "prop-nyc-001",   "Pinnacle 60",     "Steak / Raw bar", null);
    }

    private void addRestaurant(String id, String hotelId, String name, String cuisine, Integer stars) {
        restaurantHotel.put(id, hotelId);
        restaurantName.put(id, name);
        restaurantCuisine.put(id, cuisine);
        if (stars != null) restaurantStars.put(id, stars);
    }

    private void initCourses() {
        courseHotel.put("course-001", "prop-dubai-001");
        courseName.put("course-001", "Atlantis Royal Links");
        courseHotel.put("course-002", "prop-tokyo-001");
        courseName.put("course-002", "Sakura Country Club");
    }

    // ── Lookups ──────────────────────────────────────────────────────────────

    @Override
    public List<Experience> findExperiences(String hotelId, String category, LocalDate date) {
        return experiences.values().stream()
                .filter(e -> hotelId == null || e.hotelId().equals(hotelId))
                .filter(e -> category == null || e.category().equals(category))
                .filter(Experience::available)
                .collect(Collectors.toList());
    }

    @Override public Optional<Experience> findExperienceById(String id) {
        return Optional.ofNullable(experiences.get(id));
    }

    @Override
    public ExperienceAvailability availability(String experienceId, LocalDate date, int partySize) {
        Experience e = experiences.get(experienceId);
        if (e == null) {
            return new ExperienceAvailability(experienceId, date, partySize,
                    List.of(), true, date.plusDays(7));
        }
        List<TimeSlot> slots = generateSlots(experienceId, date, e.maxParticipants(), e.pricePerPerson(), 4);
        boolean fullyBooked = slots.stream().noneMatch(TimeSlot::available);
        return new ExperienceAvailability(experienceId, date, partySize, slots, fullyBooked,
                fullyBooked ? date.plusDays(1) : null);
    }

    private List<TimeSlot> generateSlots(String resourceId, LocalDate date,
                                           int totalCapacity, Money price, int slotsCount) {
        List<TimeSlot> slots = new ArrayList<>();
        String[] startTimes = {"09:00", "11:30", "14:00", "16:30", "19:00"};
        for (int i = 0; i < Math.min(slotsCount, startTimes.length); i++) {
            String start = startTimes[i];
            String end = String.format("%02d:%02d",
                    (Integer.parseInt(start.substring(0, 2)) + 1) % 24,
                    Integer.parseInt(start.substring(3, 5)));
            int remaining = Math.max(0, totalCapacity - (i % 3));
            String token = "slot-" + resourceId + "-" + date + "-" + start.replace(":", "");
            validSlotTokens.add(token);
            slots.add(new TimeSlot(token, start, end, remaining > 0, remaining,
                    price, i % 2 == 0 ? "Camille L." : null));
        }
        return slots;
    }

    @Override public boolean isSlotValid(String slotToken) {
        return slotToken != null && (validSlotTokens.contains(slotToken)
                || slotToken.startsWith("slot-"));
    }

    @Override public List<SpaTreatment> spaTreatments(String hotelId) {
        return spaTreatments.values().stream()
                .filter(t -> hotelId == null || t.hotelId().equals(hotelId))
                .collect(Collectors.toList());
    }

    @Override
    public RestaurantAvailability restaurantAvailability(String hotelId, LocalDate date, int partySize) {
        List<RestaurantAvailability.RestaurantSlots> rs = restaurantHotel.entrySet().stream()
                .filter(e -> e.getValue().equals(hotelId))
                .map(e -> {
                    String rid = e.getKey();
                    List<TimeSlot> slots = generateSlots(rid, date, 8 * partySize,
                            Money.of(0, "USD"), 5);
                    return new RestaurantAvailability.RestaurantSlots(
                            rid, restaurantName.get(rid), restaurantCuisine.get(rid),
                            restaurantStars.get(rid), slots);
                })
                .collect(Collectors.toList());
        return new RestaurantAvailability(hotelId, date, partySize, rs);
    }

    @Override
    public GolfTeeTimeAvailability golfAvailability(String hotelId, LocalDate date, int players) {
        String courseId = courseHotel.entrySet().stream()
                .filter(e -> e.getValue().equals(hotelId))
                .map(Map.Entry::getKey)
                .findFirst().orElse("course-default");
        String name = courseName.getOrDefault(courseId, "Resort Links");
        List<TimeSlot> slots = generateSlots(courseId, date, 4, Money.of(285, "USD"), 5);
        return new GolfTeeTimeAvailability(hotelId, courseId, name, date, players, slots);
    }

    // ── Bookings ─────────────────────────────────────────────────────────────

    @Override
    public ExperienceBooking bookExperience(String guestId, String experienceId,
                                              String slotToken, int participants, String specialRequests) {
        Experience e = experiences.get(experienceId);
        if (e == null) return null;
        Money total = Money.of(Double.parseDouble(e.pricePerPerson().amount()) * participants,
                e.pricePerPerson().currency());
        return persistBooking(experienceId, e.hotelId(), guestId, slotToken,
                participants, total, specialRequests);
    }

    @Override
    public ExperienceBooking bookDining(String guestId, String restaurantId,
                                         String slotToken, int partySize, String specialRequests) {
        String hotelId = restaurantHotel.getOrDefault(restaurantId, "unknown");
        Money total = Money.of(0, "USD");
        return persistBooking("dining-" + restaurantId, hotelId, guestId, slotToken,
                partySize, total, specialRequests);
    }

    @Override
    public ExperienceBooking bookGolf(String guestId, String courseId, String slotToken,
                                       int players, Boolean cartRequested) {
        String hotelId = courseHotel.getOrDefault(courseId, "unknown");
        Money total = Money.of(285.0 * players, "USD");
        String notes = (cartRequested != null && cartRequested) ? "Cart requested" : null;
        return persistBooking("golf-" + courseId, hotelId, guestId, slotToken,
                players, total, notes);
    }

    private ExperienceBooking persistBooking(String experienceId, String hotelId,
                                               String guestId, String slotToken,
                                               int participants, Money total, String notes) {
        SlotInfo info = decodeSlot(slotToken);
        String id = "exb-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "EXB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ExperienceBooking b = new ExperienceBooking(id, experienceId, guestId, hotelId,
                "CONFIRMED", info.date, info.startTime, null,
                participants, total, notes, code, OffsetDateTime.now());
        bookings.put(id, b);
        return b;
    }

    private record SlotInfo(LocalDate date, String startTime) {}

    private SlotInfo decodeSlot(String token) {
        try {
            String[] parts = token.split("-");
            // slot-<resourceId-...>-YYYY-MM-DD-HHMM
            String time  = parts[parts.length - 1];
            String day   = parts[parts.length - 2];
            String month = parts[parts.length - 3];
            String year  = parts[parts.length - 4];
            return new SlotInfo(LocalDate.of(Integer.parseInt(year), Integer.parseInt(month),
                    Integer.parseInt(day)),
                    time.substring(0, 2) + ":" + time.substring(2));
        } catch (Exception e) {
            return new SlotInfo(LocalDate.now().plusDays(1), "12:00");
        }
    }

    @Override
    public ExperienceBooking cancel(String bookingId, String reason) {
        ExperienceBooking b = bookings.get(bookingId);
        if (b == null) return null;
        b.cancel(reason);
        return b;
    }

    @Override public Optional<ExperienceBooking> findBookingById(String id) {
        return Optional.ofNullable(bookings.get(id));
    }

    @Override
    public List<ExperienceBooking> findBookingsByGuestId(String guestId, Boolean upcoming) {
        LocalDate today = LocalDate.now();
        return bookings.values().stream()
                .filter(b -> b.getGuestId().equals(guestId))
                .filter(b -> upcoming == null
                        || (upcoming ? !b.getDate().isBefore(today) : b.getDate().isBefore(today)))
                .sorted(Comparator.comparing(ExperienceBooking::getDate))
                .collect(Collectors.toList());
    }
}
