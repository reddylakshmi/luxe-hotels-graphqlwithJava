package com.luxe.loyalty.schema.types;

import com.luxe.common.pagination.HasId;

public record LoyaltyPartner(
        String id, String name, String category, String description, String logoUrl,
        double pointsRatio, int minTransfer, Integer maxTransfer,
        int transferIncrement, boolean active
) implements HasId {
    @Override public String getId() { return id; }
}
