package com.luxe.reservations.datasource;

import com.luxe.common.scalar.Money;
import com.luxe.reservations.schema.types.*;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReservationMockDataSource implements ReservationDataSource {

    private final Map<String, Reservation> reservations = new LinkedHashMap<>();
    private final Map<String, DiningReservation> diningReservations = new LinkedHashMap<>();
    private final Map<String, SpaAppointment> spaAppointments = new LinkedHashMap<>();
    private final Map<String, TransportationBooking> transportationBookings = new LinkedHashMap<>();

    public ReservationMockDataSource() {
        initData();
    }

    private void initData() {
        LocalDate today = LocalDate.now();

        // Paris — upcoming confirmed
        Reservation r1 = res("res-001", "LUX-2025-100001", "guest-001", "prop-paris-001",
                "rt-paris-dlx-001", "rate-paris-dlx-flex", null,
                today.plusDays(10), today.plusDays(15), 5, 2, 0,
                Money.of(520, "EUR"), Money.of(2600, "EUR"), "EUR",
                "CONFIRMED", "Late check-in requested", today.minusDays(30));
        r1.setLoyaltyContext(new ReservationLoyaltyContext("LUX0001234567", "GOLD",
                2600, null, 1.5, 5));
        reservations.put(r1.getId(), r1);

        // London — upcoming confirmed, check-in in 3 days (eligible for online check-in)
        Reservation r2 = res("res-002", "LUX-2025-100002", "guest-002", "prop-london-001",
                "rt-london-dlx-001", "rate-london-dlx-flex", null,
                today.plusDays(3), today.plusDays(6), 3, 2, 0,
                Money.of(580, "GBP"), Money.of(1740, "GBP"), "GBP",
                "CONFIRMED", null, today.minusDays(15));
        r2.setLoyaltyContext(new ReservationLoyaltyContext("LUX0002345678", "PLATINUM",
                1740, null, 2.0, 3));
        reservations.put(r2.getId(), r2);

        // Tokyo — far future, anniversary
        Reservation r3 = res("res-003", "LUX-2025-100003", "guest-001", "prop-tokyo-001",
                "rt-tokyo-suite-001", "rate-tokyo-suite-flex", null,
                today.plusDays(60), today.plusDays(65), 5, 2, 1,
                Money.of(180000, "JPY"), Money.of(900000, "JPY"), "JPY",
                "CONFIRMED", "Celebrating anniversary — surprise decoration please", today.minusDays(5));
        r3.setLoyaltyContext(new ReservationLoyaltyContext("LUX0001234567", "GOLD",
                9000, null, 1.5, 5));
        reservations.put(r3.getId(), r3);

        // Dubai — currently checked in
        Reservation r4 = res("res-004", "LUX-2025-100004", "guest-003", "prop-dubai-001",
                "rt-dubai-dlx-001", "rate-dubai-dlx-flex", "1205",
                today.minusDays(2), today.plusDays(3), 5, 2, 0,
                Money.of(1100, "AED"), Money.of(5500, "AED"), "AED",
                "CHECKED_IN", null, today.minusDays(20));
        r4.setCheckedInAt(today.minusDays(2).atTime(15, 30).atOffset(ZoneOffset.UTC));
        r4.setDigitalKey(new DigitalKey("res-004", "DK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "ACTIVE", today.minusDays(2).atTime(15, 30).atOffset(ZoneOffset.UTC),
                today.plusDays(3).atTime(12, 0).atOffset(ZoneOffset.UTC),
                List.of("1205")));
        r4.setPaymentSummary(new PaymentSummary("CREDIT_CARD", "4242", "Visa",
                "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                today.minusDays(2).atTime(10, 0).atOffset(ZoneOffset.UTC),
                Money.of(5500, "AED"), "AED", "CAPTURED"));
        reservations.put(r4.getId(), r4);

        // NYC — checked out
        Reservation r5 = res("res-005", "LUX-2025-100005", "guest-004", "prop-nyc-001",
                "rt-nyc-dlx-001", "rate-nyc-dlx-flex", "2501",
                today.minusDays(10), today.minusDays(7), 3, 1, 0,
                Money.of(720, "USD"), Money.of(2160, "USD"), "USD",
                "CHECKED_OUT", null, today.minusDays(30));
        r5.setCheckedInAt(today.minusDays(10).atTime(16, 0).atOffset(ZoneOffset.UTC));
        r5.setCheckedOutAt(today.minusDays(7).atTime(11, 30).atOffset(ZoneOffset.UTC));
        r5.setPaymentSummary(new PaymentSummary("CREDIT_CARD", "5678", "Mastercard",
                "AUTH-5678", today.minusDays(10).atTime(16, 0).atOffset(ZoneOffset.UTC),
                Money.of(2496, "USD"), "USD", "CAPTURED"));
        r5.setFolio(buildFolio("res-005", "USD", Money.of(720, "USD"), 3));
        reservations.put(r5.getId(), r5);

        // Paris — cancelled
        Reservation r6 = res("res-006", "LUX-2025-100006", "guest-005", "prop-paris-001",
                "rt-paris-std-001", "rate-paris-std-flex", null,
                today.plusDays(20), today.plusDays(23), 3, 2, 0,
                Money.of(350, "EUR"), Money.of(1050, "EUR"), "EUR",
                "CANCELLED", null, today.minusDays(45));
        r6.setCancellation(new CancellationRecord(
                today.minusDays(10).atTime(9, 0).atOffset(ZoneOffset.UTC),
                "Change of travel plans", "GUEST",
                Money.of(1050, "EUR"), "REFUNDED",
                "CXL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()));
        reservations.put(r6.getId(), r6);

        // London — upcoming, family trip
        Reservation r7 = res("res-007", "LUX-2025-100007", "guest-005", "prop-london-001",
                "rt-london-std-001", "rate-london-std-flex", null,
                today.plusDays(30), today.plusDays(35), 5, 2, 1,
                Money.of(380, "GBP"), Money.of(1900, "GBP"), "GBP",
                "CONFIRMED", "High floor preferred", today.minusDays(10));
        reservations.put(r7.getId(), r7);

        // Dubai — suite, future family trip
        Reservation r8 = res("res-008", "LUX-2025-100008", "guest-002", "prop-dubai-001",
                "rt-dubai-suite-001", "rate-dubai-suite-flex", null,
                today.plusDays(90), today.plusDays(95), 5, 2, 2,
                Money.of(3800, "AED"), Money.of(19000, "AED"), "AED",
                "CONFIRMED", "Family trip with children aged 4 and 7", today.minusDays(3));
        reservations.put(r8.getId(), r8);

        // Dining reservations
        diningReservations.put("din-001", new DiningReservation("din-001", "res-001",
                "rest-paris-001", "Le Grand Salon", today.plusDays(11), "19:30", 2,
                "CONFIRMED", "Window table preferred", "DIN-001234",
                OffsetDateTime.now().minusDays(25)));
        diningReservations.put("din-002", new DiningReservation("din-002", "res-004",
                "rest-dubai-001", "Sky Terrace", today.plusDays(1), "20:00", 2,
                "CONFIRMED", null, "DIN-002345",
                OffsetDateTime.now().minusDays(2)));

        // Spa appointments
        spaAppointments.put("spa-001", new SpaAppointment("spa-001", "res-001",
                "spa-treat-001", "Signature Luxe Massage", "Marie Dupont",
                today.plusDays(12), "10:00", 90, Money.of(280, "EUR"),
                "CONFIRMED", "SPA-001234", OffsetDateTime.now().minusDays(20)));
        spaAppointments.put("spa-002", new SpaAppointment("spa-002", "res-004",
                "spa-treat-002", "Arabian Hammam Experience", null,
                today.plusDays(2), "14:00", 120, Money.of(450, "AED"),
                "CONFIRMED", "SPA-002345", OffsetDateTime.now().minusDays(1)));

        // Transportation bookings
        transportationBookings.put("trans-001", new TransportationBooking("trans-001", "res-001",
                "AIRPORT_PICKUP", "CDG Airport Terminal 2E", "Hôtel Luxe Paris",
                today.plusDays(10).atTime(14, 0).atOffset(ZoneOffset.UTC),
                "Mercedes E-Class", Money.of(95, "EUR"), "CONFIRMED",
                "Flight AF1234 arriving 13:30", "TRN-001234",
                OffsetDateTime.now().minusDays(28)));
        transportationBookings.put("trans-002", new TransportationBooking("trans-002", "res-004",
                "AIRPORT_DROPOFF", "Atlantis The Palm Hotel", "DXB Airport Terminal 3",
                today.plusDays(3).atTime(10, 0).atOffset(ZoneOffset.UTC),
                "BMW 7 Series", Money.of(220, "AED"), "CONFIRMED",
                null, "TRN-002345",
                OffsetDateTime.now().minusDays(2)));
    }

    private Reservation res(String id, String conf, String guestId, String hotelId,
                              String rtId, String rateId, String roomNumber,
                              LocalDate checkIn, LocalDate checkOut, int nights,
                              int adults, int children, Money nightly, Money total, String currency,
                              String status, String specialReqs, LocalDate createdDate) {
        return new Reservation(id, conf, guestId, hotelId, rtId, rateId, roomNumber,
                checkIn, checkOut, nights, adults, children, nightly, total, currency,
                status, specialReqs, createdDate.atTime(10, 0).atOffset(ZoneOffset.UTC));
    }

    private Folio buildFolio(String reservationId, String currency, Money nightlyRate, int nights) {
        double nightly = Double.parseDouble(nightlyRate.amount());
        double subtotal = nightly * nights;
        double taxes = subtotal * 0.15;
        List<Folio.FolioLineItem> items = new ArrayList<>();
        LocalDate checkIn = LocalDate.now().minusDays(10);
        for (int i = 0; i < nights; i++) {
            items.add(new Folio.FolioLineItem("fli-" + i, checkIn.plusDays(i),
                    "Room Charge", nightlyRate, "ROOM", 1));
        }
        items.add(new Folio.FolioLineItem("fli-tax", checkIn.plusDays(nights - 1),
                "Taxes & Fees", Money.of(taxes, currency), "TAX", 1));
        return new Folio("folio-" + reservationId, reservationId, currency, items,
                Money.of(subtotal, currency), Money.of(taxes, currency),
                Money.of(subtotal + taxes, currency), "SETTLED");
    }

    // ── Interface implementations ─────────────────────────────────────────────

    @Override
    public Optional<Reservation> findById(String id) {
        return Optional.ofNullable(reservations.get(id));
    }

    @Override
    public Optional<Reservation> findByConfirmationNumber(String confirmationNumber, String guestLastName) {
        return reservations.values().stream()
                .filter(r -> r.getConfirmationNumber().equals(confirmationNumber))
                .findFirst();
    }

    @Override
    public List<Reservation> findByGuestId(String guestId, String status, String hotelId, String sortBy) {
        return reservations.values().stream()
                .filter(r -> r.getGuestId().equals(guestId))
                .filter(r -> status == null || r.getStatus().equals(status))
                .filter(r -> hotelId == null || r.getHotelId().equals(hotelId))
                .sorted("CHECK_IN_ASC".equals(sortBy)
                        ? Comparator.comparing(Reservation::getCheckIn)
                        : Comparator.comparing(Reservation::getCheckIn).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByHotelId(String hotelId) {
        return reservations.values().stream()
                .filter(r -> r.getHotelId().equals(hotelId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findUpcoming(String hotelId) {
        LocalDate today = LocalDate.now();
        return reservations.values().stream()
                .filter(r -> r.getHotelId().equals(hotelId))
                .filter(r -> ("CONFIRMED".equals(r.getStatus()) || "CHECKED_IN".equals(r.getStatus()))
                        && !r.getCheckIn().isBefore(today))
                .sorted(Comparator.comparing(Reservation::getCheckIn))
                .collect(Collectors.toList());
    }

    @Override
    public Reservation create(Map<String, Object> input, String guestId) {
        String hotelId    = (String) input.get("hotelId");
        String roomTypeId = (String) input.get("roomTypeId");
        LocalDate checkIn  = LocalDate.parse((String) input.get("checkIn"));
        LocalDate checkOut = LocalDate.parse((String) input.get("checkOut"));
        int adults   = ((Number) input.getOrDefault("adults", 1)).intValue();
        int children = ((Number) input.getOrDefault("children", 0)).intValue();
        int nights   = (int) checkIn.until(checkOut).getDays();

        String currency = hotelCurrency(hotelId);
        double baseNightly = 500.0;
        Money nightly = Money.of(baseNightly, currency);
        Money total   = Money.of(baseNightly * nights, currency);

        String id   = "res-" + UUID.randomUUID().toString().substring(0, 8);
        String conf = "LUX-" + LocalDate.now().getYear() + "-" + (100000 + new Random().nextInt(899999));

        Reservation r = new Reservation(id, conf, guestId, hotelId, roomTypeId, "rate-new",
                null, checkIn, checkOut, nights, adults, children, nightly, total, currency,
                "PENDING_PAYMENT", null, OffsetDateTime.now());
        r.setSource("WEB");
        reservations.put(id, r);
        return r;
    }

    @Override
    public Reservation confirm(String id, String paymentMethodId, String guestProfileId) {
        Reservation r = reservations.get(id);
        if (r == null) return null;
        r.setStatus("CONFIRMED");
        r.setPaymentSummary(new PaymentSummary("CREDIT_CARD", "****",
                "Visa", "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                OffsetDateTime.now(), r.getTotalAmount(), r.getCurrency(), "AUTHORIZED"));
        r.setUpdatedAt(OffsetDateTime.now());
        return r;
    }

    @Override
    public Reservation modify(String id, Map<String, Object> input) {
        Reservation r = reservations.get(id);
        if (r == null) return null;
        if (input.containsKey("checkIn")) r.setCheckIn(LocalDate.parse((String) input.get("checkIn")));
        if (input.containsKey("checkOut")) r.setCheckOut(LocalDate.parse((String) input.get("checkOut")));
        if (r.getCheckIn() != null && r.getCheckOut() != null)
            r.setNights((int) r.getCheckIn().until(r.getCheckOut()).getDays());
        if (input.containsKey("adults")) r.setAdults(((Number) input.get("adults")).intValue());
        r.setStatus("MODIFIED");
        r.setUpdatedAt(OffsetDateTime.now());
        r.rebuildRateBreakdown();
        return r;
    }

    @Override
    public Reservation cancel(String id, String reason, boolean acceptPenalty) {
        Reservation r = reservations.get(id);
        if (r == null) return null;
        String newStatus = acceptPenalty ? "CANCELLED_WITH_FEE" : "CANCELLED";
        r.setStatus(newStatus);
        r.setCancellation(new CancellationRecord(
                OffsetDateTime.now(), reason, "GUEST",
                acceptPenalty ? null : r.getTotalAmount(),
                acceptPenalty ? "PENDING" : "REFUNDED",
                "CXL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()));
        r.setUpdatedAt(OffsetDateTime.now());
        return r;
    }

    @Override
    public Reservation mobileCheckIn(String id, Map<String, Object> input) {
        Reservation r = reservations.get(id);
        if (r == null) return null;
        r.setStatus("CHECKED_IN");
        r.setCheckedInAt(OffsetDateTime.now());
        r.setUpdatedAt(OffsetDateTime.now());
        String keyCode = "DK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        r.setDigitalKey(new DigitalKey(id, keyCode, "ACTIVE", OffsetDateTime.now(),
                r.getCheckOut().atTime(12, 0).atOffset(ZoneOffset.UTC),
                r.getRoom() != null ? List.of(r.getRoom().number()) : List.of()));
        return r;
    }

    @Override
    public Reservation expressCheckout(String id) {
        Reservation r = reservations.get(id);
        if (r == null) return null;
        r.setStatus("CHECKED_OUT");
        r.setCheckedOutAt(OffsetDateTime.now());
        r.setUpdatedAt(OffsetDateTime.now());
        if (r.getFolio() == null) {
            r.setFolio(buildFolio(id, r.getCurrency(), r.getNightlyRate(), r.getNights()));
        }
        if (r.getDigitalKey() != null) r.getDigitalKey().activate();
        return r;
    }

    @Override
    public Reservation requestUpgrade(String id, Map<String, Object> input) {
        Reservation r = reservations.get(id);
        if (r == null) return null;
        String prefRoomTypeId = (String) input.getOrDefault("preferredRoomTypeId", "suite");
        String reason = (String) input.get("reason");
        r.setRoomUpgradeRequest(new RoomUpgradeRequest(
                "upg-" + UUID.randomUUID().toString().substring(0, 8),
                prefRoomTypeId, "Suite Room", "PENDING", null, null, reason));
        r.setUpdatedAt(OffsetDateTime.now());
        return r;
    }

    @Override
    public Reservation applyGiftCard(String id, String giftCardCode) {
        Reservation r = reservations.get(id);
        if (r == null) return null;
        r.setUpdatedAt(OffsetDateTime.now());
        return r;
    }

    @Override
    public CheckInEligibility checkInEligibility(String reservationId) {
        Reservation r = reservations.get(reservationId);
        if (r == null) return new CheckInEligibility(false, reservationId,
                LocalDate.now(), List.of("Reservation not found"), false, false);

        List<String> reasons = new ArrayList<>();
        long daysUntil = LocalDate.now().until(r.getCheckIn()).getDays();
        boolean eligible = "CONFIRMED".equals(r.getStatus()) || "MODIFIED".equals(r.getStatus());

        if (!eligible) reasons.add("Reservation is not in a confirmable state");
        if (daysUntil > 3) reasons.add("Online check-in available within 3 days of arrival");
        if (daysUntil < 0) reasons.add("Check-in date has passed");

        boolean onlineOk = eligible && daysUntil >= 0 && daysUntil <= 3;
        return new CheckInEligibility(eligible, reservationId, r.getCheckIn(),
                reasons, onlineOk, onlineOk);
    }

    @Override
    public List<DiningReservation> findDiningByGuestId(String guestId, Boolean upcoming) {
        LocalDate today = LocalDate.now();
        Set<String> guestResIds = reservations.values().stream()
                .filter(r -> r.getGuestId().equals(guestId))
                .map(Reservation::getId)
                .collect(Collectors.toSet());
        return diningReservations.values().stream()
                .filter(d -> guestResIds.contains(d.getReservationId()))
                .filter(d -> upcoming == null || upcoming
                        ? !d.getDate().isBefore(today) : d.getDate().isBefore(today))
                .sorted(Comparator.comparing(DiningReservation::getDate))
                .collect(Collectors.toList());
    }

    @Override
    public DiningReservation createDining(Map<String, Object> input, String guestId) {
        String id = "din-" + UUID.randomUUID().toString().substring(0, 8);
        String conf = "DIN-" + (100000 + new Random().nextInt(899999));
        DiningReservation d = new DiningReservation(id,
                (String) input.get("reservationId"),
                (String) input.get("restaurantId"),
                "Restaurant",
                LocalDate.parse((String) input.get("date")),
                (String) input.get("time"),
                ((Number) input.getOrDefault("partySize", 2)).intValue(),
                "CONFIRMED", (String) input.get("specialRequests"), conf, OffsetDateTime.now());
        diningReservations.put(id, d);
        return d;
    }

    @Override
    public List<SpaAppointment> findSpaByGuestId(String guestId, Boolean upcoming) {
        LocalDate today = LocalDate.now();
        Set<String> guestResIds = reservations.values().stream()
                .filter(r -> r.getGuestId().equals(guestId))
                .map(Reservation::getId)
                .collect(Collectors.toSet());
        return spaAppointments.values().stream()
                .filter(s -> guestResIds.contains(s.getReservationId()))
                .filter(s -> upcoming == null || upcoming
                        ? !s.getDate().isBefore(today) : s.getDate().isBefore(today))
                .sorted(Comparator.comparing(SpaAppointment::getDate))
                .collect(Collectors.toList());
    }

    @Override
    public SpaAppointment bookSpa(Map<String, Object> input, String guestId) {
        String id = "spa-" + UUID.randomUUID().toString().substring(0, 8);
        String conf = "SPA-" + (100000 + new Random().nextInt(899999));
        String currency = "USD";
        String resId = (String) input.get("reservationId");
        Reservation r = reservations.get(resId);
        if (r != null) currency = r.getCurrency();
        SpaAppointment s = new SpaAppointment(id, resId,
                (String) input.get("treatmentId"),
                "Spa Treatment",
                (String) input.get("therapistPreference"),
                LocalDate.parse((String) input.get("date")),
                (String) input.get("time"), 60,
                Money.of(200, currency), "CONFIRMED", conf, OffsetDateTime.now());
        spaAppointments.put(id, s);
        return s;
    }

    @Override
    public List<TransportationBooking> findTransportByGuestId(String guestId, Boolean upcoming) {
        OffsetDateTime now = OffsetDateTime.now();
        Set<String> guestResIds = reservations.values().stream()
                .filter(r -> r.getGuestId().equals(guestId))
                .map(Reservation::getId)
                .collect(Collectors.toSet());
        return transportationBookings.values().stream()
                .filter(t -> guestResIds.contains(t.getReservationId()))
                .filter(t -> upcoming == null || upcoming
                        ? !t.getScheduledAt().isBefore(now) : t.getScheduledAt().isBefore(now))
                .sorted(Comparator.comparing(TransportationBooking::getScheduledAt))
                .collect(Collectors.toList());
    }

    @Override
    public TransportationBooking bookTransport(Map<String, Object> input, String guestId) {
        String id = "trans-" + UUID.randomUUID().toString().substring(0, 8);
        String conf = "TRN-" + (100000 + new Random().nextInt(899999));
        String currency = "USD";
        String resId = (String) input.get("reservationId");
        Reservation r = reservations.get(resId);
        if (r != null) currency = r.getCurrency();
        TransportationBooking t = new TransportationBooking(id, resId,
                (String) input.get("type"),
                (String) input.get("pickupLocation"),
                (String) input.get("dropoffLocation"),
                OffsetDateTime.parse((String) input.get("scheduledAt")),
                (String) input.get("vehicleType"),
                Money.of(100, currency), "CONFIRMED",
                (String) input.get("notes"), conf, OffsetDateTime.now());
        transportationBookings.put(id, t);
        return t;
    }

    private String hotelCurrency(String hotelId) {
        return switch (hotelId != null ? hotelId : "") {
            case "prop-paris-001" -> "EUR";
            case "prop-tokyo-001" -> "JPY";
            case "prop-dubai-001" -> "AED";
            case "prop-london-001" -> "GBP";
            default -> "USD";
        };
    }
}
