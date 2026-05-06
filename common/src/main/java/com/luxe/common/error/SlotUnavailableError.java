package com.luxe.common.error;

import java.util.List;

public record SlotUnavailableError(
        String code, String message, String slotToken, List<String> suggestedAlternativeTokens
) {
    public SlotUnavailableError(String slotToken, List<String> alternatives) {
        this("SLOT_UNAVAILABLE",
                "The selected time slot is no longer available",
                slotToken,
                alternatives != null ? alternatives : List.of());
    }
}
