package com.luxe.common.scalar;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(String amount, String currency) {

    public static Money of(double amount, String currency) {
        return new Money(String.format("%.2f", amount), currency);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount.setScale(2, RoundingMode.HALF_UP).toPlainString(), currency);
    }

    public static Money fromString(String value) {
        String[] parts = value.trim().split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Money format, expected 'amount currency': " + value);
        }
        return new Money(parts[0], parts[1]);
    }

    public BigDecimal amountAsBigDecimal() {
        return new BigDecimal(amount);
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add Money with different currencies");
        }
        return Money.of(amountAsBigDecimal().add(other.amountAsBigDecimal()), currency);
    }

    public Money multiply(double factor) {
        return Money.of(amountAsBigDecimal().multiply(BigDecimal.valueOf(factor)), currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
