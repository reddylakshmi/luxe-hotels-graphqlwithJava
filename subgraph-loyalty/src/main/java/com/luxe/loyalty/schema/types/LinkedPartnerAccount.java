package com.luxe.loyalty.schema.types;

import java.time.OffsetDateTime;

public record LinkedPartnerAccount(
        String id, LoyaltyPartner partner, String partnerAccountNumber,
        OffsetDateTime linkedAt, OffsetDateTime lastSyncedAt, String status
) {}
