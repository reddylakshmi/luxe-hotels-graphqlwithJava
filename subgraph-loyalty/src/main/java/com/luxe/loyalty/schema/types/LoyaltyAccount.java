package com.luxe.loyalty.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

public class LoyaltyAccount implements HasId {

    private final String id;
    private final String loyaltyNumber;
    private final String guestId;
    private String status;
    private String tier;
    private int pointsAvailable;
    private int pointsPending;
    private int pointsExpiringSoon;
    private int lifetimePoints;
    private int lifetimeNights;
    private final LocalDate memberSince;
    private final String referralCode;
    private int qualifyingNights;
    private double qualifyingSpend;
    private final String currency;
    private OffsetDateTime updatedAt;

    private final List<Certificate> certificates = new ArrayList<>();
    private final List<LinkedPartnerAccount> linkedPartners = new ArrayList<>();

    public LoyaltyAccount(String id, String loyaltyNumber, String guestId, String tier,
                          String status, int pointsAvailable, int pointsPending,
                          int pointsExpiringSoon, int lifetimePoints, int lifetimeNights,
                          LocalDate memberSince, String referralCode,
                          int qualifyingNights, double qualifyingSpend, String currency) {
        this.id = id; this.loyaltyNumber = loyaltyNumber; this.guestId = guestId;
        this.tier = tier; this.status = status;
        this.pointsAvailable = pointsAvailable; this.pointsPending = pointsPending;
        this.pointsExpiringSoon = pointsExpiringSoon;
        this.lifetimePoints = lifetimePoints; this.lifetimeNights = lifetimeNights;
        this.memberSince = memberSince; this.referralCode = referralCode;
        this.qualifyingNights = qualifyingNights; this.qualifyingSpend = qualifyingSpend;
        this.currency = currency;
        this.updatedAt = OffsetDateTime.now();
    }

    @Override public String getId() { return id; }
    public String getLoyaltyNumber() { return loyaltyNumber; }
    public String getGuestId() { return guestId; }
    public String getStatus() { return status; }
    public String getTier() { return tier; }
    public int getPointsAvailable() { return pointsAvailable; }
    public int getPointsPending() { return pointsPending; }
    public int getPointsExpiringSoon() { return pointsExpiringSoon; }
    public int getLifetimePoints() { return lifetimePoints; }
    public int getLifetimeNights() { return lifetimeNights; }
    public LocalDate getMemberSince() { return memberSince; }
    public String getReferralCode() { return referralCode; }
    public int getQualifyingNights() { return qualifyingNights; }
    public double getQualifyingSpend() { return qualifyingSpend; }
    public String getCurrency() { return currency; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    // GuestProfile federation reference: emit { id }
    public Map<String, Object> getGuest() { return Map.of("id", guestId); }

    public List<Certificate> getCertificatesRaw() { return certificates; }
    public List<LinkedPartnerAccount> getLinkedPartners() { return linkedPartners; }

    public void setStatus(String s) { this.status = s; touch(); }
    public void setTier(String t) { this.tier = t; touch(); }
    public void addAvailable(int n) { this.pointsAvailable += n; touch(); }
    public void deductAvailable(int n) { this.pointsAvailable -= n; touch(); }
    public void addLifetime(int n) { this.lifetimePoints += n; touch(); }
    public void addQualifyingNights(int n) { this.qualifyingNights += n; touch(); }
    public void addCertificate(Certificate c) { this.certificates.add(c); touch(); }
    public void addLinkedPartner(LinkedPartnerAccount p) { this.linkedPartners.add(p); touch(); }
    public void extendExpiry() {
        this.pointsExpiringSoon = 0;
        touch();
    }
    private void touch() { this.updatedAt = OffsetDateTime.now(); }
}
