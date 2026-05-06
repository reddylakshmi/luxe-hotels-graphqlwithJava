package com.luxe.reservations.schema.types;

import com.luxe.common.scalar.Money;
import java.time.OffsetDateTime;

public record PaymentSummary(
        String method, String lastFour, String brand,
        String authorizationCode, OffsetDateTime chargedAt,
        Money amount, String currency, String status
) {}
