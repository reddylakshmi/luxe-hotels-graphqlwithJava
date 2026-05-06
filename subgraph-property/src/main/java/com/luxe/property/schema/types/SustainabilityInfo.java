package com.luxe.property.schema.types;

import java.util.List;

public record SustainabilityInfo(
        Integer ecoScore,
        List<SustainabilityCertification> certifications,
        boolean carbonOffsetProgram,
        Integer renewableEnergyPct,
        Integer localSourcingPct
) {
    public record SustainabilityCertification(String name, String issuedBy, int year) {}
}
