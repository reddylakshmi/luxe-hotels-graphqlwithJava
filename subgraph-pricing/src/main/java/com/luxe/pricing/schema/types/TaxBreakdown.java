package com.luxe.pricing.schema.types;

import com.luxe.common.scalar.Money;
import java.util.List;

public record TaxBreakdown(Money subtotal, Money taxes, Money fees, Money total,
                            List<TaxLineItem> breakdown) {
    public record TaxLineItem(String name, Money amount, String type) {}
}
