package com.luxe.pricing.schema.types;

import com.luxe.common.scalar.Money;
import java.time.OffsetDateTime;

public record GiftCardBalance(String code, Money balance, String currency,
                               OffsetDateTime expiresAt, boolean active) {}
