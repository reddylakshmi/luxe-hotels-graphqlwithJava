package com.luxe.pricing.schema.types;

import com.luxe.common.scalar.Money;
import java.time.LocalDate;

public record DateRateSummary(LocalDate date, Money lowestRate, String currency, boolean available) {}
