package com.luxe.reservations.schema.types;

import com.luxe.common.scalar.Money;
import java.util.List;

public record ReservationRateBreakdown(
        String currency,
        List<NightlyRate> nightlyRates,
        Money roomSubtotal,
        Money addOnSubtotal,
        Money promotionSavings,
        TaxBreakdown taxesAndFees,
        Money loyaltyDiscount,
        Money giftCardDiscount,
        Money totalDue,
        Money depositPaid,
        Money balanceDue,
        List<BillingLineItem> lineItems
) {
    public record NightlyRate(String date, Money rate) {}

    public record TaxBreakdown(Money subtotal, Money taxes, Money fees, Money total,
                                List<TaxLineItem> breakdown) {}

    public record TaxLineItem(String name, Money amount, String type) {}

    public record BillingLineItem(String id, String date, String description,
                                   Money amount, String category, int quantity) {}
}
