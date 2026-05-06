package com.luxe.pricing.schema.types;

public record CancellationPolicy(String type, String description, Integer deadlineHours) {}
