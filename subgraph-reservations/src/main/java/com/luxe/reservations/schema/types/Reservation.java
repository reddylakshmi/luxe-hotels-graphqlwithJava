package com.luxe.reservations.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Reservation implements HasId {
    private String id, confirmationNumber, guestId, hotelId, roomTypeId, rateId, source;
    private String status;
    private LocalDate checkIn, checkOut;
    private int nights, adults, children;
    private String currency;
    private Money nightlyRate, totalAmount;

    // Rich fields
    private ReservationRoom room;
    private ReservationRateBreakdown rateBreakdown;
    private List<ReservationAddOn> addOns = new ArrayList<>();
    private List<SpecialRequest> specialRequests = new ArrayList<>();
    private PaymentSummary paymentSummary;
    private Folio folio;
    private ReservationLoyaltyContext loyaltyContext;
    private DigitalKey digitalKey;
    private RoomUpgradeRequest roomUpgradeRequest;
    private ReservationCancellationPolicy cancellationPolicy;
    private CancellationRecord cancellation;

    private OffsetDateTime checkedInAt, checkedOutAt, createdAt, updatedAt, confirmationSentAt;

    public Reservation(String id, String confirmationNumber, String guestId, String hotelId,
                       String roomTypeId, String rateId, String roomNumber,
                       LocalDate checkIn, LocalDate checkOut, int nights,
                       int adults, int children, Money nightlyRate, Money totalAmount,
                       String currency, String status, String specialRequestText,
                       OffsetDateTime createdAt) {
        this.id = id; this.confirmationNumber = confirmationNumber;
        this.guestId = guestId; this.hotelId = hotelId; this.roomTypeId = roomTypeId;
        this.rateId = rateId; this.checkIn = checkIn; this.checkOut = checkOut;
        this.nights = nights; this.adults = adults; this.children = children;
        this.nightlyRate = nightlyRate; this.totalAmount = totalAmount; this.currency = currency;
        this.status = status; this.createdAt = createdAt; this.updatedAt = createdAt;
        this.source = "WEB";
        this.confirmationSentAt = createdAt.plusMinutes(2);

        if (roomNumber != null) {
            int floor = roomNumber.length() >= 2 ? Integer.parseInt(roomNumber.substring(0, roomNumber.length() - 2)) : 1;
            this.room = new ReservationRoom(roomNumber, floor, null, null);
        }

        if (specialRequestText != null) {
            this.specialRequests = List.of(new SpecialRequest(
                    "sr-" + id, "OTHER", specialRequestText, "ACKNOWLEDGED", null));
        }

        this.cancellationPolicy = new ReservationCancellationPolicy(
                "FREE_CANCELLATION", "Free cancellation up to 48 hours before check-in",
                48, null, null);

        buildRateBreakdown();
    }

    private void buildRateBreakdown() {
        if (nightlyRate == null || totalAmount == null) return;
        double nightly = Double.parseDouble(nightlyRate.amount());
        double subtotal = Double.parseDouble(totalAmount.amount());
        double taxRate = currency.equals("GBP") ? 0.20 : currency.equals("USD") ? 0.15 : 0.10;
        double taxes = subtotal * taxRate;
        double fees  = subtotal * 0.02;

        List<ReservationRateBreakdown.NightlyRate> nightlyRates = checkIn.datesUntil(checkOut)
                .map(d -> new ReservationRateBreakdown.NightlyRate(d.toString(), nightlyRate))
                .collect(Collectors.toList());

        ReservationRateBreakdown.TaxBreakdown taxBreakdown = new ReservationRateBreakdown.TaxBreakdown(
                Money.of(subtotal, currency),
                Money.of(taxes, currency),
                Money.of(fees, currency),
                Money.of(taxes + fees, currency),
                List.of(
                        new ReservationRateBreakdown.TaxLineItem("Value Added Tax", Money.of(taxes, currency), "TAX"),
                        new ReservationRateBreakdown.TaxLineItem("Resort Fee", Money.of(fees, currency), "FEE")
                ));

        Money total = Money.of(subtotal + taxes + fees, currency);
        List<ReservationRateBreakdown.BillingLineItem> lineItems = List.of(
                new ReservationRateBreakdown.BillingLineItem("li-room-" + id,
                        checkIn.toString(), "Room Charges (" + nights + " nights)",
                        Money.of(subtotal, currency), "ROOM", nights));

        this.rateBreakdown = new ReservationRateBreakdown(currency, nightlyRates,
                totalAmount, null, null, taxBreakdown, null, null,
                total, null, total, lineItems);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    @Override public String getId() { return id; }
    public String getConfirmationNumber() { return confirmationNumber; }
    public String getGuestId() { return guestId; }
    public String getHotelId() { return hotelId; }
    public String getRoomTypeId() { return roomTypeId; }
    public String getRateId() { return rateId; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getNights() { return nights; }
    public int getAdults() { return adults; }
    public int getChildren() { return children; }
    public String getCurrency() { return currency; }
    public Money getNightlyRate() { return nightlyRate; }
    public Money getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public String getSource() { return source; }

    // Federation reference getters — return maps with just id for Apollo Router
    public Map<String, Object> getHotel() { return Map.of("id", hotelId); }
    public Map<String, Object> getRoomType() { return Map.of("id", roomTypeId); }
    public Map<String, Object> getGuest() { return guestId != null ? Map.of("id", guestId) : null; }

    public GuestCount getGuestCount() { return new GuestCount(adults, children); }
    public ReservationRoom getRoom() { return room; }
    public ReservationRateBreakdown getRateBreakdown() { return rateBreakdown; }
    public List<ReservationAddOn> getAddOns() { return addOns; }
    public List<SpecialRequest> getSpecialRequests() { return specialRequests; }
    public PaymentSummary getPaymentSummary() { return paymentSummary; }
    public Folio getFolio() { return folio; }
    public ReservationLoyaltyContext getLoyaltyContext() { return loyaltyContext; }
    public DigitalKey getDigitalKey() { return digitalKey; }
    public RoomUpgradeRequest getRoomUpgradeRequest() { return roomUpgradeRequest; }
    public ReservationCancellationPolicy getCancellationPolicy() { return cancellationPolicy; }
    public CancellationRecord getCancellation() { return cancellation; }

    public boolean isIsRefundable() {
        return cancellationPolicy != null && "FREE_CANCELLATION".equals(cancellationPolicy.type())
                && !isCancellationDeadlinePassed();
    }

    private boolean isCancellationDeadlinePassed() {
        if (cancellationPolicy == null || cancellationPolicy.deadlineHours() == null) return false;
        OffsetDateTime deadline = checkIn.atStartOfDay().atOffset(java.time.ZoneOffset.UTC)
                .minusHours(cancellationPolicy.deadlineHours());
        return OffsetDateTime.now().isAfter(deadline);
    }

    public OffsetDateTime getCancellationDeadline() {
        if (cancellationPolicy == null || cancellationPolicy.deadlineHours() == null) return null;
        return checkIn.atStartOfDay().atOffset(java.time.ZoneOffset.UTC)
                .minusHours(cancellationPolicy.deadlineHours());
    }

    public boolean isIsCanModify() {
        return "CONFIRMED".equals(status) || "MODIFIED".equals(status);
    }

    public boolean isIsCanCheckInOnline() {
        if (!"CONFIRMED".equals(status) && !"MODIFIED".equals(status)) return false;
        long daysUntilCheckIn = LocalDate.now().until(checkIn).getDays();
        return daysUntilCheckIn >= 0 && daysUntilCheckIn <= 3;
    }

    public OffsetDateTime getCheckedInAt() { return checkedInAt; }
    public OffsetDateTime getCheckedOutAt() { return checkedOutAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getConfirmationSentAt() { return confirmationSentAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setStatus(String status) { this.status = status; }
    public void setRoom(ReservationRoom room) { this.room = room; }
    public void setRoomNumber(String roomNumber) {
        if (roomNumber == null) { this.room = null; return; }
        int floor = roomNumber.length() >= 2 ? Integer.parseInt(roomNumber.substring(0, roomNumber.length() - 2)) : 1;
        this.room = new ReservationRoom(roomNumber, floor, null, null);
    }
    public void setPaymentSummary(PaymentSummary ps) { this.paymentSummary = ps; }
    public void setFolio(Folio f) { this.folio = f; }
    public void setLoyaltyContext(ReservationLoyaltyContext lc) { this.loyaltyContext = lc; }
    public void setDigitalKey(DigitalKey dk) { this.digitalKey = dk; }
    public void setRoomUpgradeRequest(RoomUpgradeRequest r) { this.roomUpgradeRequest = r; }
    public void setCancellation(CancellationRecord cr) { this.cancellation = cr; }
    public void setCheckedInAt(OffsetDateTime t) { this.checkedInAt = t; }
    public void setCheckedOutAt(OffsetDateTime t) { this.checkedOutAt = t; }
    public void setUpdatedAt(OffsetDateTime t) { this.updatedAt = t; }
    public void setCheckIn(LocalDate d) { this.checkIn = d; }
    public void setCheckOut(LocalDate d) { this.checkOut = d; }
    public void setNights(int n) { this.nights = n; }
    public void setAdults(int a) { this.adults = a; }
    public void setSource(String s) { this.source = s; }
    public void addSpecialRequest(SpecialRequest sr) { this.specialRequests = new ArrayList<>(specialRequests); specialRequests.add(sr); }
    public void addAddOn(ReservationAddOn addOn) { this.addOns = new ArrayList<>(addOns); addOns.add(addOn); }
    public void rebuildRateBreakdown() { buildRateBreakdown(); }

    // ── Inner value type ──────────────────────────────────────────────────────

    public record GuestCount(int adults, int children) {}
}
