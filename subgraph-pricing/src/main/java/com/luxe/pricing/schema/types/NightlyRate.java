package com.luxe.pricing.schema.types;

import com.luxe.common.scalar.Money;
import java.time.LocalDate;

public record NightlyRate(LocalDate date, Money rate) {}
