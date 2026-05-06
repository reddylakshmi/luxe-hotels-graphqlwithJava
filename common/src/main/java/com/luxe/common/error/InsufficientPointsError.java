package com.luxe.common.error;

public record InsufficientPointsError(String code, int currentBalance, int requiredPoints, int shortfall) {
    public InsufficientPointsError(int currentBalance, int requiredPoints) {
        this("INSUFFICIENT_POINTS", currentBalance, requiredPoints, requiredPoints - currentBalance);
    }
}
