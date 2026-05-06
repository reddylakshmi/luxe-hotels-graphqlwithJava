package com.luxe.common.error;

public record PaymentDeclinedError(String code, String message, boolean retryWithNewCard) {
    public PaymentDeclinedError(String message, boolean retryWithNewCard) {
        this("PAYMENT_DECLINED", message, retryWithNewCard);
    }
}
