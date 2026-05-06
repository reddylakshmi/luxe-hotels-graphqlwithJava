package com.luxe.corporate.datasource;

import com.luxe.common.scalar.Money;
import com.luxe.corporate.schema.types.*;
import com.luxe.corporate.schema.types.TravelPolicy.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CorporateMockDataSource implements CorporateDataSource {

    private final Map<String, CorporateAccount> accounts = new LinkedHashMap<>();
    private final Map<String, CorporateAccount> byContract = new LinkedHashMap<>();
    private final Map<String, String> guestToAccount = new LinkedHashMap<>();
    private final Map<String, TravelApproval> approvals = new LinkedHashMap<>();
    private final Random random = new Random();

    public CorporateMockDataSource() {
        initAccounts();
        initApprovals();
    }

    // ── Seed: accounts ───────────────────────────────────────────────────────

    private void initAccounts() {
        OffsetDateTime now = OffsetDateTime.now();

        CorporateAccount acme = new CorporateAccount(
                "corp-001", "Acme Pharma", "LUX-CORP-2024-001", "PREMIER", "Pharmaceutical", "ACTIVE",
                new CorporateContact("Maria Chen", "events@acme.example", "+1-415-555-0142", "Director, Travel"),
                new CorporateContact("Accounts Payable", "ap@acme.example", null, "Billing"),
                List.of(new CorporateContact("Sarah Lim", "sarah@acme.example", null, "Travel Manager"),
                        new CorporateContact("Renee Park", "renee@acme.example", null, "Travel Manager")),
                LocalDate.of(2024, 1, 1), LocalDate.of(2026, 12, 31),
                Money.of(842500, "USD"),
                now.minusYears(2));
        acme.setTravelPolicy(buildPolicy("policy-001", "corp-001",
                List.of(new RateCap("New York", "US", Money.of(620, "USD"), null),
                        new RateCap("London",   "GB", Money.of(540, "GBP"), null),
                        new RateCap("Paris",    "FR", Money.of(560, "EUR"), null)),
                Money.of(700, "USD"), Money.of(900, "USD"),
                List.of(new ApprovalLevel(1, Money.of(900, "USD"), "TRAVEL_MANAGER", "sarah@acme.example"),
                        new ApprovalLevel(2, Money.of(1500, "USD"), "VP_FINANCE", "renee@acme.example")),
                List.of("STANDARD", "DELUXE"),
                true, 14, true,
                List.of(), List.of("prop-paris-001", "prop-london-001", "prop-nyc-001"),
                Money.of(85, "USD")));
        acme.addTraveler(new CorporateTraveler("trv-001", "corp-001", "guest-001",
                "ACME-1042", "R&D", "RD-1000", "rd-mgr@acme.example", "BUSINESS", true));
        acme.addTraveler(new CorporateTraveler("trv-002", "corp-001", "guest-002",
                "ACME-2018", "Legal", "LG-2000", "legal-mgr@acme.example", "BUSINESS", true));
        acme.addNegotiatedRate(new NegotiatedRate("nrt-001", "corp-001", "prop-paris-001",
                "Acme Corporate Paris", "ACME-PAR", 18.0, Money.of(520, "EUR"),
                LocalDate.of(2024, 1, 1), LocalDate.of(2026, 12, 31), List.of(), true));
        acme.addNegotiatedRate(new NegotiatedRate("nrt-002", "corp-001", "prop-london-001",
                "Acme Corporate London", "ACME-LON", 16.0, Money.of(485, "GBP"),
                LocalDate.of(2024, 1, 1), LocalDate.of(2026, 12, 31), List.of(), true));
        accounts.put(acme.getId(), acme);
        byContract.put(acme.getContractNumber(), acme);
        guestToAccount.put("guest-001", acme.getId());
        guestToAccount.put("guest-002", acme.getId());

        CorporateAccount greenfield = new CorporateAccount(
                "corp-002", "Greenfield Capital", "LUX-CORP-2025-002", "ELITE", "Financial Services", "ACTIVE",
                new CorporateContact("Tomás Rivera", "tomas@greenfield.example", "+1-212-555-0177", "Head of Operations"),
                new CorporateContact("Greenfield AP", "ap@greenfield.example", null, "Billing"),
                List.of(new CorporateContact("Lara Hayes", "lara@greenfield.example", null, "Travel Manager")),
                LocalDate.of(2025, 3, 1), LocalDate.of(2028, 2, 28),
                Money.of(1_580_000, "USD"),
                now.minusYears(1));
        greenfield.setTravelPolicy(buildPolicy("policy-002", "corp-002",
                List.of(new RateCap("New York", "US", Money.of(900, "USD"), null),
                        new RateCap("London",   "GB", Money.of(820, "GBP"), null),
                        new RateCap("Tokyo",    "JP", Money.of(120000, "JPY"), null)),
                Money.of(1100, "USD"), Money.of(1500, "USD"),
                List.of(new ApprovalLevel(1, Money.of(1500, "USD"), "TRAVEL_MANAGER", "lara@greenfield.example"),
                        new ApprovalLevel(2, Money.of(3000, "USD"), "CFO", "cfo@greenfield.example")),
                List.of("DELUXE", "SUITE"),
                false, null, true,
                List.of(), List.of("prop-nyc-001", "prop-london-001", "prop-tokyo-001"),
                Money.of(120, "USD")));
        greenfield.addTraveler(new CorporateTraveler("trv-003", "corp-002", "guest-007",
                "GFC-1003", "Investments", "INV-100", "inv-mgr@greenfield.example", "FIRST", true));
        greenfield.addNegotiatedRate(new NegotiatedRate("nrt-003", "corp-002", "prop-nyc-001",
                "Greenfield Manhattan", "GFC-NYC", 22.0, Money.of(685, "USD"),
                LocalDate.of(2025, 3, 1), LocalDate.of(2028, 2, 28), List.of(), true));
        accounts.put(greenfield.getId(), greenfield);
        byContract.put(greenfield.getContractNumber(), greenfield);
        guestToAccount.put("guest-007", greenfield.getId());

        CorporateAccount tanaka = new CorporateAccount(
                "corp-003", "Tanaka Industries", "LUX-CORP-2023-005", "PREFERRED", "Manufacturing", "ACTIVE",
                new CorporateContact("Yuki Tanaka", "yuki@tanaka.example", "+81-3-5555-0148", "Travel Lead"),
                null,
                List.of(),
                LocalDate.of(2023, 6, 1), LocalDate.of(2026, 5, 31),
                Money.of(420000, "USD"),
                now.minusYears(2).minusMonths(11));
        tanaka.setTravelPolicy(buildPolicy("policy-003", "corp-003",
                List.of(new RateCap("Tokyo", "JP", Money.of(85000, "JPY"), null)),
                Money.of(550, "USD"), Money.of(700, "USD"),
                List.of(new ApprovalLevel(1, Money.of(700, "USD"), "TRAVEL_MANAGER", "yuki@tanaka.example")),
                List.of("STANDARD", "DELUXE"),
                true, 21, false,
                List.of(), List.of("prop-tokyo-001"), Money.of(60, "USD")));
        accounts.put(tanaka.getId(), tanaka);
        byContract.put(tanaka.getContractNumber(), tanaka);
    }

    private TravelPolicy buildPolicy(String id, String accountId, List<RateCap> caps,
                                       Money max, Money apprAbove,
                                       List<ApprovalLevel> chain, List<String> allowed,
                                       boolean advReq, Integer advDays, boolean reqJust,
                                       List<String> blocked, List<String> preferred, Money perDiem) {
        return new TravelPolicy(id, accountId, caps, max, apprAbove, chain,
                allowed, advReq, advDays, reqJust, blocked, preferred, perDiem);
    }

    // ── Seed: approvals ──────────────────────────────────────────────────────

    private void initApprovals() {
        OffsetDateTime now = OffsetDateTime.now();
        approvals.put("apr-001", new TravelApproval("apr-001", "corp-001", "trv-001", "res-101",
                "PENDING", Money.of(1100, "USD"),
                List.of(new TravelApproval.PolicyException(
                        "MAX_NIGHTLY_RATE", "Quoted USD 1,100/nt exceeds USD 700 cap", "WARNING"),
                        new TravelApproval.PolicyException(
                        "ADVANCE_BOOKING", "Booked 8 days ahead vs 14-day rule", "INFO")),
                "Client meeting moved up due to product launch",
                now.minusHours(20)));
        approvals.put("apr-002", new TravelApproval("apr-002", "corp-002", "trv-003", "res-301",
                "PENDING", Money.of(2400, "USD"),
                List.of(new TravelApproval.PolicyException(
                        "MAX_NIGHTLY_RATE", "Suite at USD 2,400/nt exceeds USD 1,100 cap", "BLOCKING")),
                "Hosting board offsite — suite required",
                now.minusHours(36)));
    }

    // ── Lookups ──────────────────────────────────────────────────────────────

    @Override public Optional<CorporateAccount> findById(String id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override public Optional<CorporateAccount> findByGuestId(String guestId) {
        String accountId = guestToAccount.get(guestId);
        return accountId == null ? Optional.empty() : Optional.ofNullable(accounts.get(accountId));
    }

    @Override public Optional<CorporateAccount> findByContractNumber(String contractNumber) {
        return Optional.ofNullable(byContract.get(contractNumber));
    }

    @Override public Optional<TravelPolicy> findPolicy(String accountId) {
        return Optional.ofNullable(accounts.get(accountId)).map(CorporateAccount::getTravelPolicy);
    }

    @Override
    public List<NegotiatedRate> findRates(String accountId, String hotelId) {
        CorporateAccount a = accounts.get(accountId);
        if (a == null) return List.of();
        return a.getNegotiatedRates().stream()
                .filter(r -> hotelId == null || r.hotelId().equals(hotelId))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public CorporateAccount enroll(Map<String, Object> input) {
        String id = "corp-" + UUID.randomUUID().toString().substring(0, 8);
        String contract = "LUX-CORP-" + LocalDate.now().getYear() + "-"
                + String.format("%03d", random.nextInt(900) + 100);
        Map<String, Object> primary = (Map<String, Object>) input.get("primaryContact");
        Map<String, Object> billing = (Map<String, Object>) input.get("billingContact");
        CorporateContact pc = new CorporateContact(
                (String) primary.get("name"), (String) primary.get("email"),
                (String) primary.get("phone"), (String) primary.get("title"));
        CorporateContact bc = billing == null ? null
                : new CorporateContact((String) billing.get("name"), (String) billing.get("email"),
                        (String) billing.get("phone"), (String) billing.get("title"));
        CorporateAccount a = new CorporateAccount(id,
                (String) input.get("companyName"), contract,
                (String) input.getOrDefault("tier", "STANDARD"),
                (String) input.get("industry"),
                "PENDING_REVIEW", pc, bc, List.of(),
                LocalDate.parse((String) input.get("contractStartDate")),
                LocalDate.parse((String) input.get("contractEndDate")),
                Money.of(0, "USD"), OffsetDateTime.now());
        a.setTravelPolicy(buildPolicy("policy-" + id, id,
                List.of(), Money.of(500, "USD"), Money.of(750, "USD"),
                List.of(new ApprovalLevel(1, Money.of(750, "USD"), "TRAVEL_MANAGER",
                        pc.email())),
                List.of("STANDARD"), false, null, true,
                List.of(), List.of(), Money.of(75, "USD")));
        accounts.put(id, a);
        byContract.put(contract, a);
        return a;
    }

    @SuppressWarnings("unchecked")
    @Override
    public TravelPolicy updatePolicy(String accountId, Map<String, Object> input) {
        CorporateAccount a = accounts.get(accountId);
        if (a == null) return null;
        TravelPolicy p = a.getTravelPolicy();
        Money max = input.get("maxNightlyRateUsd") != null
                ? Money.of(((Number) input.get("maxNightlyRateUsd")).doubleValue(), "USD") : null;
        Money apprAbove = input.get("requiresApprovalAboveUsd") != null
                ? Money.of(((Number) input.get("requiresApprovalAboveUsd")).doubleValue(), "USD") : null;
        Money perDiem = input.get("perDiemMealsUsd") != null
                ? Money.of(((Number) input.get("perDiemMealsUsd")).doubleValue(), "USD") : null;

        List<Map<String, Object>> capsRaw = (List<Map<String, Object>>) input.get("rateCaps");
        List<RateCap> caps = capsRaw == null ? null : capsRaw.stream().map(m ->
                new RateCap((String) m.get("city"), (String) m.get("countryCode"),
                        Money.of(((Number) m.get("maxNightlyRateUsd")).doubleValue(), "USD"),
                        (String) m.get("appliesToTier"))).toList();

        List<Map<String, Object>> chainRaw = (List<Map<String, Object>>) input.get("approvalChain");
        List<ApprovalLevel> chain = chainRaw == null ? null : chainRaw.stream().map(m ->
                new ApprovalLevel(((Number) m.get("level")).intValue(),
                        Money.of(((Number) m.get("thresholdUsd")).doubleValue(), "USD"),
                        (String) m.get("approverRole"),
                        (String) m.get("approverEmail"))).toList();

        p.apply(max, apprAbove, caps, chain,
                (List<String>) input.get("allowedRoomCategories"),
                (Boolean) input.get("advanceBookingRequired"),
                input.get("advanceBookingDays") != null
                        ? ((Number) input.get("advanceBookingDays")).intValue() : null,
                (Boolean) input.get("requiresBusinessJustification"),
                (List<String>) input.get("blockedHotelIds"),
                (List<String>) input.get("preferredHotelIds"),
                perDiem);
        return p;
    }

    @Override
    public CorporateTraveler addTraveler(Map<String, Object> input) {
        String accountId = (String) input.get("accountId");
        CorporateAccount a = accounts.get(accountId);
        if (a == null) return null;
        String id = "trv-" + UUID.randomUUID().toString().substring(0, 8);
        CorporateTraveler t = new CorporateTraveler(id, accountId,
                (String) input.get("guestId"),
                (String) input.get("employeeId"),
                (String) input.get("department"),
                (String) input.get("costCenter"),
                (String) input.get("managerEmail"),
                (String) input.getOrDefault("travelClass", "BUSINESS"),
                true);
        a.addTraveler(t);
        if (t.getGuestId() != null) guestToAccount.put(t.getGuestId(), accountId);
        return t;
    }

    @Override
    public boolean removeTraveler(String travelerId) {
        for (CorporateAccount a : accounts.values()) {
            for (CorporateTraveler t : a.getTravelers()) {
                if (t.getId().equals(travelerId) && t.isActive()) {
                    t.deactivate();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<TravelApproval> pendingApprovals(String accountId) {
        return approvals.values().stream()
                .filter(ap -> "PENDING".equals(ap.getStatus()))
                .filter(ap -> accountId == null || ap.getAccountId().equals(accountId))
                .sorted(Comparator.comparing(TravelApproval::getRequestedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override public Optional<TravelApproval> findApproval(String id) {
        return Optional.ofNullable(approvals.get(id));
    }

    @Override
    public TravelApproval reviewApproval(String id, String decision, String reviewer, String notes) {
        TravelApproval a = approvals.get(id);
        if (a == null) return null;
        a.decide("APPROVE".equals(decision) ? "APPROVED" : "DENIED", reviewer, notes);
        return a;
    }

    @Override
    public TravelReport report(String accountId, String period) {
        CorporateAccount a = accounts.get(accountId);
        Money total = a != null ? a.getYtdSpend() : Money.of(0, "USD");
        return new TravelReport(accountId, period,
                184, total, 462, 0.91, 17,
                List.of(new TravelReport.DestinationSpend("Paris", "FR", 38, Money.of(285000, "USD")),
                        new TravelReport.DestinationSpend("London", "GB", 32, Money.of(232000, "USD")),
                        new TravelReport.DestinationSpend("New York", "US", 41, Money.of(312000, "USD"))),
                List.of(new TravelReport.BrandSpend("Luxe", 184, total)),
                List.of(new TravelReport.MonthlySpend("2026-01", 28, Money.of(118000, "USD")),
                        new TravelReport.MonthlySpend("2026-02", 22, Money.of(96000, "USD")),
                        new TravelReport.MonthlySpend("2026-03", 31, Money.of(141000, "USD")),
                        new TravelReport.MonthlySpend("2026-04", 24, Money.of(102000, "USD"))),
                124850.0,
                OffsetDateTime.now());
    }
}
