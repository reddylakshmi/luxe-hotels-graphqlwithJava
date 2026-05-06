package com.luxe.corporate.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TravelPolicy implements HasId {

    private final String id, accountId;
    private List<RateCap> rateCaps;
    private Money maxNightlyRateUsd;
    private Money requiresApprovalAboveUsd;
    private List<ApprovalLevel> approvalChain;
    private List<String> allowedRoomCategories;
    private boolean advanceBookingRequired;
    private Integer advanceBookingDays;
    private boolean requiresBusinessJustification;
    private List<String> blockedHotelIds;
    private List<String> preferredHotelIds;
    private Money perDiemMeals;
    private OffsetDateTime updatedAt;

    public TravelPolicy(String id, String accountId, List<RateCap> rateCaps,
                         Money maxNightlyRateUsd, Money requiresApprovalAboveUsd,
                         List<ApprovalLevel> approvalChain,
                         List<String> allowedRoomCategories,
                         boolean advanceBookingRequired, Integer advanceBookingDays,
                         boolean requiresBusinessJustification,
                         List<String> blockedHotelIds, List<String> preferredHotelIds,
                         Money perDiemMeals) {
        this.id = id; this.accountId = accountId;
        this.rateCaps = new ArrayList<>(rateCaps);
        this.maxNightlyRateUsd = maxNightlyRateUsd;
        this.requiresApprovalAboveUsd = requiresApprovalAboveUsd;
        this.approvalChain = new ArrayList<>(approvalChain);
        this.allowedRoomCategories = new ArrayList<>(allowedRoomCategories);
        this.advanceBookingRequired = advanceBookingRequired;
        this.advanceBookingDays = advanceBookingDays;
        this.requiresBusinessJustification = requiresBusinessJustification;
        this.blockedHotelIds = new ArrayList<>(blockedHotelIds);
        this.preferredHotelIds = new ArrayList<>(preferredHotelIds);
        this.perDiemMeals = perDiemMeals;
        this.updatedAt = OffsetDateTime.now();
    }

    @Override public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public List<RateCap> getRateCaps() { return rateCaps; }
    public Money getMaxNightlyRateUsd() { return maxNightlyRateUsd; }
    public Money getRequiresApprovalAboveUsd() { return requiresApprovalAboveUsd; }
    public List<ApprovalLevel> getApprovalChain() { return approvalChain; }
    public List<String> getAllowedRoomCategories() { return allowedRoomCategories; }
    public boolean isAdvanceBookingRequired() { return advanceBookingRequired; }
    public Integer getAdvanceBookingDays() { return advanceBookingDays; }
    public boolean isRequiresBusinessJustification() { return requiresBusinessJustification; }
    public Money getPerDiemMeals() { return perDiemMeals; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public List<Map<String, Object>> getBlockedHotels() {
        return blockedHotelIds.stream().<Map<String, Object>>map(id -> Map.of("id", id)).toList();
    }
    public List<Map<String, Object>> getPreferredHotels() {
        return preferredHotelIds.stream().<Map<String, Object>>map(id -> Map.of("id", id)).toList();
    }

    public void apply(Money max, Money apprAbove, List<RateCap> caps,
                       List<ApprovalLevel> chain, List<String> allowed,
                       Boolean advReq, Integer advDays, Boolean reqJust,
                       List<String> blocked, List<String> preferred, Money perDiem) {
        if (max != null) this.maxNightlyRateUsd = max;
        if (apprAbove != null) this.requiresApprovalAboveUsd = apprAbove;
        if (caps != null) this.rateCaps = caps;
        if (chain != null) this.approvalChain = chain;
        if (allowed != null) this.allowedRoomCategories = allowed;
        if (advReq != null) this.advanceBookingRequired = advReq;
        if (advDays != null) this.advanceBookingDays = advDays;
        if (reqJust != null) this.requiresBusinessJustification = reqJust;
        if (blocked != null) this.blockedHotelIds = blocked;
        if (preferred != null) this.preferredHotelIds = preferred;
        if (perDiem != null) this.perDiemMeals = perDiem;
        this.updatedAt = OffsetDateTime.now();
    }

    public record RateCap(String city, String countryCode, Money maxNightlyRate, String appliesToTier) {}

    public record ApprovalLevel(int level, Money threshold, String approverRole, String approverEmail) {}
}
