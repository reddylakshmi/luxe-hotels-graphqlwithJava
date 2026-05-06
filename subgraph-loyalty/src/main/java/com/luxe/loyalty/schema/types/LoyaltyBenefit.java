package com.luxe.loyalty.schema.types;

public record LoyaltyBenefit(
        String code, String name, String description,
        String category, String tier
) {}
