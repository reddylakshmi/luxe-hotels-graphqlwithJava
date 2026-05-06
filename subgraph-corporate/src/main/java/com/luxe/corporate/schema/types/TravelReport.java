package com.luxe.corporate.schema.types;

import com.luxe.common.scalar.Money;
import java.time.OffsetDateTime;
import java.util.List;

public record TravelReport(
        String accountId, String period,
        int totalTrips, Money totalSpend, int totalNights,
        double policyComplianceRate, int outOfPolicyTrips,
        List<DestinationSpend> topDestinations,
        List<BrandSpend> spendByBrand, List<MonthlySpend> spendByMonth,
        double carbonFootprintKg, OffsetDateTime generatedAt
) {
    public record DestinationSpend(String city, String countryCode, int trips, Money totalSpend) {}
    public record BrandSpend(String brand, int trips, Money totalSpend) {}
    public record MonthlySpend(String month, int trips, Money totalSpend) {}
}
