package com.luxe.guest.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.OffsetDateTime;
import java.time.YearMonth;

public class PaymentMethod implements HasId {
    private final String id, type, brand, lastFour, holderName, pspToken;
    private final int expiryMonth, expiryYear;
    private final GuestAddress billingAddress;
    private boolean isDefault;
    private final OffsetDateTime addedAt;

    public PaymentMethod(String id, String type, String brand, String lastFour, String holderName,
                         String pspToken, int expiryMonth, int expiryYear,
                         GuestAddress billingAddress, boolean isDefault, OffsetDateTime addedAt) {
        this.id = id; this.type = type; this.brand = brand; this.lastFour = lastFour;
        this.holderName = holderName; this.pspToken = pspToken;
        this.expiryMonth = expiryMonth; this.expiryYear = expiryYear;
        this.billingAddress = billingAddress; this.isDefault = isDefault; this.addedAt = addedAt;
    }

    @Override public String getId() { return id; }
    public String getType() { return type; }
    public String getBrand() { return brand; }
    public String getLastFour() { return lastFour; }
    public String getHolderName() { return holderName; }
    public String getPspToken() { return pspToken; }
    public int getExpiryMonth() { return expiryMonth; }
    public int getExpiryYear() { return expiryYear; }
    public GuestAddress getBillingAddress() { return billingAddress; }
    public boolean isIsDefault() { return isDefault; }
    public boolean isIsExpired() {
        YearMonth expiry = YearMonth.of(expiryYear, expiryMonth);
        return expiry.isBefore(YearMonth.now());
    }
    public OffsetDateTime getAddedAt() { return addedAt; }
    public void setDefault(boolean d) { this.isDefault = d; }
}
