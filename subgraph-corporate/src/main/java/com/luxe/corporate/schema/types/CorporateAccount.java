package com.luxe.corporate.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class CorporateAccount implements HasId {

    private final String id, contractNumber, industry;
    private String companyName, status, tier;
    private CorporateContact primaryContact;
    private CorporateContact billingContact;
    private final List<CorporateContact> travelManagers = new ArrayList<>();
    private final List<CorporateTraveler> travelers = new ArrayList<>();
    private TravelPolicy travelPolicy;
    private final List<NegotiatedRate> negotiatedRates = new ArrayList<>();
    private final LocalDate contractStartDate, contractEndDate;
    private Money ytdSpend;
    private final OffsetDateTime createdAt;

    public CorporateAccount(String id, String companyName, String contractNumber, String tier,
                              String industry, String status, CorporateContact primary,
                              CorporateContact billing, List<CorporateContact> managers,
                              LocalDate contractStartDate, LocalDate contractEndDate,
                              Money ytdSpend, OffsetDateTime createdAt) {
        this.id = id; this.companyName = companyName; this.contractNumber = contractNumber;
        this.tier = tier; this.industry = industry; this.status = status;
        this.primaryContact = primary; this.billingContact = billing;
        if (managers != null) this.travelManagers.addAll(managers);
        this.contractStartDate = contractStartDate; this.contractEndDate = contractEndDate;
        this.ytdSpend = ytdSpend;
        this.createdAt = createdAt;
    }

    @Override public String getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getContractNumber() { return contractNumber; }
    public String getTier() { return tier; }
    public String getIndustry() { return industry; }
    public String getStatus() { return status; }
    public CorporateContact getPrimaryContact() { return primaryContact; }
    public CorporateContact getBillingContact() { return billingContact; }
    public List<CorporateContact> getTravelManagers() { return travelManagers; }
    public List<CorporateTraveler> getTravelers() { return travelers; }
    public TravelPolicy getTravelPolicy() { return travelPolicy; }
    public List<NegotiatedRate> getNegotiatedRates() { return negotiatedRates; }
    public int getTotalTravelers() {
        return (int) travelers.stream().filter(CorporateTraveler::isActive).count();
    }
    public LocalDate getContractStartDate() { return contractStartDate; }
    public LocalDate getContractEndDate() { return contractEndDate; }
    public Money getYtdSpend() { return ytdSpend; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setStatus(String s) { this.status = s; }
    public void setTier(String t) { this.tier = t; }
    public void setTravelPolicy(TravelPolicy p) { this.travelPolicy = p; }
    public void addTraveler(CorporateTraveler t) { this.travelers.add(t); }
    public void addNegotiatedRate(NegotiatedRate r) { this.negotiatedRates.add(r); }
    public void addYtdSpend(Money m) {
        this.ytdSpend = m;
    }
}
