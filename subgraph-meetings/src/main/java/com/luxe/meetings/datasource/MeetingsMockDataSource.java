package com.luxe.meetings.datasource;

import com.luxe.common.scalar.Money;
import com.luxe.meetings.schema.types.*;
import com.luxe.meetings.schema.types.EventSpace.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MeetingsMockDataSource implements MeetingsDataSource {

    private final Map<String, EventSpace> spaces = new LinkedHashMap<>();
    private final Map<String, CateringMenu> caterings = new LinkedHashMap<>();
    private final Map<String, RFP> rfps = new LinkedHashMap<>();
    private final Map<String, GroupBlock> groupBlocks = new LinkedHashMap<>();

    public MeetingsMockDataSource() {
        initSpaces();
        initCaterings();
        initRFPs();
        initGroupBlocks();
    }

    // ── Event spaces ─────────────────────────────────────────────────────────

    private void initSpaces() {
        spaces.put("evs-001", buildSpace("evs-001", "prop-paris-001",
                "Salle Versailles", "BALLROOM",
                "Grand ballroom with chandelier, terrace access and full blackout capability",
                450, 4500.0, 4.6, true, true, 3, true, "EUR",
                Map.of("THEATER", 480, "BANQUET", 320, "CLASSROOM", 240,
                        "RECEPTION", 600, "U_SHAPE", 60),
                Money.of(12000, "EUR"), Money.of(7800, "EUR"),
                Money.of(1800, "EUR"), Money.of(950, "EUR"),
                Money.of(580, "EUR"), Money.of(15000, "EUR"),
                "Three-phase 400A", 1000, 24,
                avDefaults("EUR")));

        spaces.put("evs-002", buildSpace("evs-002", "prop-paris-001",
                "Bibliothèque Boardroom", "BOARDROOM",
                "Wood-paneled boardroom for up to 24 with lake-facing windows",
                95, 720.0, 3.6, true, false, 1, false, "EUR",
                Map.of("BOARDROOM", 24, "U_SHAPE", 22, "CLASSROOM", 28),
                Money.of(2400, "EUR"), Money.of(1500, "EUR"),
                Money.of(450, "EUR"), Money.of(220, "EUR"),
                Money.of(140, "EUR"), Money.of(2500, "EUR"),
                "Single-phase 32A", 500, 4,
                avDefaults("EUR")));

        spaces.put("evs-003", buildSpace("evs-003", "prop-london-001",
                "Mayfair Grand Ballroom", "BALLROOM",
                "London's most photographed ballroom with crystal chandeliers",
                380, 3850.0, 5.2, false, true, 2, true, "GBP",
                Map.of("THEATER", 400, "BANQUET", 280, "CLASSROOM", 220,
                        "RECEPTION", 520, "HOLLOW_SQUARE", 70),
                Money.of(9800, "GBP"), Money.of(6500, "GBP"),
                Money.of(1500, "GBP"), Money.of(800, "GBP"),
                Money.of(420, "GBP"), Money.of(12000, "GBP"),
                "Three-phase 400A", 1000, 28,
                avDefaults("GBP")));

        spaces.put("evs-004", buildSpace("evs-004", "prop-tokyo-001",
                "Sakura Atrium", "BALLROOM",
                "Light-filled atrium space with retractable Japanese garden access",
                420, 4200.0, 5.0, true, true, 1, true, "JPY",
                Map.of("THEATER", 450, "BANQUET", 280, "CLASSROOM", 240,
                        "RECEPTION", 580, "CRESCENT_ROUNDS", 200),
                Money.of(2_400_000, "JPY"), Money.of(1_500_000, "JPY"),
                Money.of(380_000, "JPY"), Money.of(220_000, "JPY"),
                Money.of(150_000, "JPY"), Money.of(2_800_000, "JPY"),
                "Three-phase 400A", 800, 16,
                avDefaults("JPY")));

        spaces.put("evs-005", buildSpace("evs-005", "prop-dubai-001",
                "Atlantis Royal Pavilion", "BALLROOM",
                "Modular pavilion with indoor-outdoor reception flow",
                550, 5400.0, 6.0, true, true, 4, true, "AED",
                Map.of("THEATER", 600, "BANQUET", 400, "CLASSROOM", 320,
                        "RECEPTION", 900),
                Money.of(58000, "AED"), Money.of(38000, "AED"),
                Money.of(8500, "AED"), Money.of(4200, "AED"),
                Money.of(2200, "AED"), Money.of(95000, "AED"),
                "Three-phase 400A", 1000, 32,
                avDefaults("AED")));

        spaces.put("evs-006", buildSpace("evs-006", "prop-nyc-001",
                "Pinnacle Sky Studio", "STUDIO",
                "Floor-to-ceiling glass studio with panoramic Manhattan view",
                160, 1500.0, 3.4, true, true, 1, false, "USD",
                Map.of("RECEPTION", 180, "THEATER", 130, "BANQUET", 120,
                        "CLASSROOM", 80, "BOARDROOM", 30),
                Money.of(7500, "USD"), Money.of(4800, "USD"),
                Money.of(1200, "USD"), Money.of(650, "USD"),
                Money.of(420, "USD"), Money.of(8500, "USD"),
                "Single-phase 60A", 1000, 8,
                avDefaults("USD")));
    }

    private EventSpace buildSpace(String id, String hotelId, String name, String category,
                                    String description, int capacity, double sqFt, double ceilingFt,
                                    boolean naturalLight, boolean blackout, int rooms, boolean divisible,
                                    String currency, Map<String, Integer> capacityMap,
                                    Money fullDay, Money halfDay, Money hourly,
                                    Money setupFee, Money cleaningFee, Money fbMin,
                                    String power, int internetMbps, Integer riggingPoints,
                                    List<AVEquipment> av) {
        List<CapacityStyle> caps = capacityMap.entrySet().stream()
                .map(e -> new CapacityStyle(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        TechnicalSpecs specs = new TechnicalSpecs(power, internetMbps, riggingPoints,
                3.0, true, "Class A");
        EventSpaceRateCard rate = new EventSpaceRateCard(fullDay, halfDay, hourly,
                setupFee, cleaningFee, fbMin, currency);
        return new EventSpace(id, hotelId, name, description, category, caps,
                sqFt, sqFt * 0.0929, ceilingFt, naturalLight, blackout,
                rooms, divisible, specs, av, false, rate,
                List.of("https://content.luxehotels.example/events/" + id + ".jpg"),
                "https://content.luxehotels.example/events/" + id + "-floor.pdf");
    }

    private List<AVEquipment> avDefaults(String currency) {
        return List.of(
                new AVEquipment("PROJECTION", "4K Laser Projector", "Sony VPL-GTZ380",
                        2, true, null),
                new AVEquipment("AUDIO", "Digital Mixing Console + Wireless Mics",
                        "Yamaha QL5", 1, true, null),
                new AVEquipment("LIGHTING", "Architectural LED Wash Package",
                        null, 24, true, null),
                new AVEquipment("VIDEO_CONFERENCING", "Polycom Studio X70",
                        "Studio X70", 1, false, Money.of(450, currency)),
                new AVEquipment("TRANSLATION", "Simultaneous Interpretation Booth",
                        null, 1, false, Money.of(1200, currency)));
    }

    // ── Catering menus ───────────────────────────────────────────────────────

    private void initCaterings() {
        caterings.put("cat-001", new CateringMenu("cat-001", "prop-paris-001",
                "Le Grand Banquet", "Four-course French banquet with sommelier-paired wines",
                Money.of(220, "EUR"), 30,
                List.of(new CateringMenu.CateringCourse("Amuse-bouche",
                            "Champagne foam, sea bream"),
                        new CateringMenu.CateringCourse("Entrée",
                            "Confit de canard, pomme purée"),
                        new CateringMenu.CateringCourse("Plat principal",
                            "Wagyu beef, sauce bordelaise"),
                        new CateringMenu.CateringCourse("Dessert",
                            "Soufflé Grand Marnier")),
                List.of("Sommelier wine pairing", "Champagne", "Coffee/Tea")));

        caterings.put("cat-002", new CateringMenu("cat-002", "prop-london-001",
                "Mayfair Reception", "Stand-up canapé reception with bespoke gin bar",
                Money.of(120, "GBP"), 50,
                List.of(new CateringMenu.CateringCourse("Cold canapés", "10 varieties"),
                        new CateringMenu.CateringCourse("Hot canapés", "8 varieties"),
                        new CateringMenu.CateringCourse("Sweet canapés", "5 varieties")),
                List.of("Gin bar", "Champagne service", "Soft drinks")));

        caterings.put("cat-003", new CateringMenu("cat-003", "prop-dubai-001",
                "Royal Buffet", "Themed buffet showcasing Emirati and pan-Asian cuisines",
                Money.of(580, "AED"), 100,
                List.of(new CateringMenu.CateringCourse("Mezze", "Hummus, mutabal, fattoush"),
                        new CateringMenu.CateringCourse("Hot mains",
                            "Slow-roasted lamb, seared seabass, biryani"),
                        new CateringMenu.CateringCourse("Dessert station",
                            "Umm Ali, kunafa, gelato")),
                List.of("Mocktail bar", "Premium juices", "Coffee/Tea")));
    }

    // ── RFPs ─────────────────────────────────────────────────────────────────

    private void initRFPs() {
        OffsetDateTime now = OffsetDateTime.now();
        RFP r1 = new RFP("rfp-001", "RFP-2026-001001", "PROPOSAL_SENT",
                "Maria Chen", "Acme Pharma", "events@acme.example", "+1-415-555-0142",
                "Acme Q2 Leadership Summit", "BOARD_RETREAT",
                LocalDate.now().plusDays(45), LocalDate.now().plusDays(48),
                85, 95, List.of("prop-paris-001", "prop-london-001"),
                List.of(new RFP.SpaceRequirement(
                        "Plenary", "THEATER", 85, 24.0, "08:30")),
                "Vegan and gluten-free options for ~30% of attendees",
                "Need on-site simultaneous interpretation in Spanish and Mandarin",
                now.minusDays(7));
        r1.setStatus("PROPOSAL_SENT", "Proposal sent for Paris and London", "Concierge bot");
        r1.addResponse(new RFP.RFPResponse(
                "rsp-001", "prop-paris-001", "PROPOSED",
                List.of("evs-001"), 95, Money.of(620, "EUR"),
                Money.of(15000, "EUR"),
                "Available with full ballroom and three breakout boardrooms",
                now.minusDays(5), now.plusDays(20)));
        r1.addResponse(new RFP.RFPResponse(
                "rsp-002", "prop-london-001", "PROPOSED",
                List.of("evs-003"), 95, Money.of(580, "GBP"),
                Money.of(12000, "GBP"),
                "Mayfair ballroom available with terrace access",
                now.minusDays(5), now.plusDays(20)));
        rfps.put(r1.getId(), r1);

        RFP r2 = new RFP("rfp-002", "RFP-2026-001002", "SUBMITTED",
                "Tomás Rivera", "Greenfield Capital", "events@greenfield.example", "+1-212-555-0177",
                "Greenfield Annual Investor Summit", "CONFERENCE",
                LocalDate.now().plusDays(120), LocalDate.now().plusDays(123),
                240, 280, List.of("prop-nyc-001", "prop-london-001"),
                List.of(new RFP.SpaceRequirement(
                            "Plenary",   "THEATER",   240, 32.0, "08:00"),
                        new RFP.SpaceRequirement(
                            "Breakouts", "BOARDROOM",  30,  8.0, "13:00")),
                "Daily plated lunch and reception, vegetarian-forward",
                null, now.minusDays(2));
        rfps.put(r2.getId(), r2);

        RFP r3 = new RFP("rfp-003", "RFP-2026-001003", "ACCEPTED",
                "Yuki Tanaka", "Tanaka Industries", "events@tanaka.example", "+81-3-5555-0148",
                "Tanaka Industries 50th Anniversary Gala", "SOCIAL_GALA",
                LocalDate.now().plusDays(70), LocalDate.now().plusDays(70),
                460, null, List.of("prop-tokyo-001"),
                List.of(new RFP.SpaceRequirement(
                        "Reception + dinner", "BANQUET", 460, 6.0, "18:30")),
                "Five-course tasting menu with sake pairing",
                "Live shamisen ensemble during cocktail hour",
                now.minusDays(15));
        r3.setStatus("ACCEPTED", "Tanaka has signed contract for Tokyo Sakura Atrium", "Concierge bot");
        rfps.put(r3.getId(), r3);
    }

    // ── Group blocks ─────────────────────────────────────────────────────────

    private void initGroupBlocks() {
        groupBlocks.put("blk-001", new GroupBlock("blk-001", "rfp-003", "prop-tokyo-001",
                "TANAKA50", LocalDate.now().plusDays(69), LocalDate.now().plusDays(71),
                95, 12, Money.of(78000, "JPY"), LocalDate.now().plusDays(40), "DEFINITE"));
        groupBlocks.put("blk-002", new GroupBlock("blk-002", "rfp-001", "prop-paris-001",
                "ACMEQ2", LocalDate.now().plusDays(44), LocalDate.now().plusDays(49),
                70, 6, Money.of(620, "EUR"), LocalDate.now().plusDays(20), "TENTATIVE"));
    }

    // ── Lookups ──────────────────────────────────────────────────────────────

    @Override
    public List<EventSpace> findSpacesByHotel(String hotelId, Map<String, Object> filter) {
        Integer minCapacity = filter == null ? null
                : filter.get("minCapacity") != null ? ((Number) filter.get("minCapacity")).intValue() : null;
        String setup = filter == null ? null : (String) filter.get("setup");
        Boolean naturalLight = filter == null ? null : (Boolean) filter.get("naturalLight");
        String category = filter == null ? null : (String) filter.get("category");
        return spaces.values().stream()
                .filter(s -> s.hotelId().equals(hotelId))
                .filter(s -> category == null || s.category().equals(category))
                .filter(s -> naturalLight == null || s.naturalLight() == naturalLight)
                .filter(s -> minCapacity == null
                        || s.capacityStyles().stream().anyMatch(c -> c.capacity() >= minCapacity))
                .filter(s -> setup == null
                        || s.capacityStyles().stream().anyMatch(c -> c.setup().equals(setup)))
                .collect(Collectors.toList());
    }

    @Override public Optional<EventSpace> findSpaceById(String id) {
        return Optional.ofNullable(spaces.get(id));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<EventSpace> searchSpaces(Map<String, Object> input) {
        Integer attendees = input.get("attendees") != null
                ? ((Number) input.get("attendees")).intValue() : 0;
        String setup = (String) input.get("setup");
        List<String> hotelIds = (List<String>) input.get("hotelIds");
        return spaces.values().stream()
                .filter(s -> hotelIds == null || hotelIds.isEmpty() || hotelIds.contains(s.hotelId()))
                .filter(s -> setup == null
                        || s.capacityStyles().stream().anyMatch(c -> c.setup().equals(setup)
                            && c.capacity() >= attendees))
                .collect(Collectors.toList());
    }

    @Override
    public List<CateringMenu> findCateringMenus(String hotelId) {
        return caterings.values().stream()
                .filter(m -> m.hotelId().equals(hotelId))
                .collect(Collectors.toList());
    }

    @Override public Optional<RFP> findRFPById(String id) { return Optional.ofNullable(rfps.get(id)); }

    @Override
    public List<RFP> findRFPsByOrganizer(String email, String status) {
        return rfps.values().stream()
                .filter(r -> r.getContactEmail().equalsIgnoreCase(email))
                .filter(r -> status == null || r.getStatus().equals(status))
                .sorted(Comparator.comparing(RFP::getSubmittedAt).reversed())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public RFP submitRFP(Map<String, Object> input, String organizerEmail) {
        String id = "rfp-" + UUID.randomUUID().toString().substring(0, 8);
        String number = "RFP-" + LocalDate.now().getYear() + "-" + (100000 + new Random().nextInt(899999));
        List<Map<String, Object>> reqsRaw = (List<Map<String, Object>>) input.get("spaceRequirements");
        List<RFP.SpaceRequirement> reqs = reqsRaw == null ? List.of()
                : reqsRaw.stream().map(m -> new RFP.SpaceRequirement(
                        (String) m.get("name"),
                        (String) m.get("setup"),
                        ((Number) m.get("attendees")).intValue(),
                        ((Number) m.get("durationHours")).doubleValue(),
                        (String) m.get("startTime"))).toList();
        RFP rfp = new RFP(id, number, "SUBMITTED",
                (String) input.get("organizer"),
                (String) input.get("organization"),
                (String) input.getOrDefault("contactEmail", organizerEmail),
                (String) input.get("contactPhone"),
                (String) input.get("eventName"),
                (String) input.get("eventType"),
                LocalDate.parse((String) input.get("startDate")),
                LocalDate.parse((String) input.get("endDate")),
                ((Number) input.get("attendees")).intValue(),
                input.get("guestRoomsPerNight") != null
                        ? ((Number) input.get("guestRoomsPerNight")).intValue() : null,
                (List<String>) input.getOrDefault("preferredHotelIds", List.of()),
                reqs,
                (String) input.get("cateringRequirements"),
                (String) input.get("additionalRequirements"),
                OffsetDateTime.now());
        rfps.put(id, rfp);
        return rfp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public RFP updateRFP(String rfpId, Map<String, Object> input) {
        RFP rfp = rfps.get(rfpId);
        if (rfp == null) return null;
        LocalDate startDate = input.get("startDate") != null
                ? LocalDate.parse((String) input.get("startDate")) : null;
        LocalDate endDate = input.get("endDate") != null
                ? LocalDate.parse((String) input.get("endDate")) : null;
        Integer attendees = input.get("attendees") != null
                ? ((Number) input.get("attendees")).intValue() : null;
        Integer rooms = input.get("guestRoomsPerNight") != null
                ? ((Number) input.get("guestRoomsPerNight")).intValue() : null;
        List<Map<String, Object>> reqsRaw = (List<Map<String, Object>>) input.get("spaceRequirements");
        List<RFP.SpaceRequirement> reqs = reqsRaw == null ? null
                : reqsRaw.stream().map(m -> new RFP.SpaceRequirement(
                        (String) m.get("name"),
                        (String) m.get("setup"),
                        ((Number) m.get("attendees")).intValue(),
                        ((Number) m.get("durationHours")).doubleValue(),
                        (String) m.get("startTime"))).toList();
        rfp.update(startDate, endDate, attendees, rooms, reqs,
                (String) input.get("cateringRequirements"),
                (String) input.get("additionalRequirements"));
        return rfp;
    }

    @Override
    public RFP cancelRFP(String rfpId, String reason, String actor) {
        RFP rfp = rfps.get(rfpId);
        if (rfp == null) return null;
        rfp.setStatus("CANCELLED", reason, actor);
        return rfp;
    }

    @Override public Optional<GroupBlock> findGroupBlockById(String id) {
        return Optional.ofNullable(groupBlocks.get(id));
    }

    @Override
    public GroupBlock createGroupBlock(Map<String, Object> input) {
        String id = "blk-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "GRP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Money rate = Money.of(((Number) input.get("rate")).doubleValue(),
                (String) input.get("currency"));
        GroupBlock block = new GroupBlock(id,
                (String) input.get("rfpId"),
                (String) input.get("hotelId"),
                code,
                LocalDate.parse((String) input.get("startDate")),
                LocalDate.parse((String) input.get("endDate")),
                ((Number) input.get("rooms")).intValue(),
                0, rate,
                LocalDate.parse((String) input.get("cutoffDate")),
                "TENTATIVE");
        groupBlocks.put(id, block);
        return block;
    }
}
