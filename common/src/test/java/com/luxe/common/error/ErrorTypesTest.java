package com.luxe.common.error;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorTypesTest {

    @Test
    void not_found_error_uses_canonical_code() {
        NotFoundError err = new NotFoundError("Hotel", "prop-paris-001");
        assertThat(err.code()).isEqualTo("NOT_FOUND");
        assertThat(err.resourceType()).isEqualTo("Hotel");
        assertThat(err.message()).contains("Hotel").contains("prop-paris-001");
    }

    @Test
    void validation_error_default_code() {
        ValidationError err = new ValidationError("missing field",
                List.of(new FieldError("name", "Required")));
        assertThat(err.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(err.fieldErrors()).hasSize(1);
        assertThat(err.fieldErrors().get(0).field()).isEqualTo("name");
    }

    @Test
    void validation_error_explicit_code_is_preserved() {
        ValidationError err = new ValidationError("X1", "explicit", List.of());
        assertThat(err.code()).isEqualTo("X1");
        assertThat(err.message()).isEqualTo("explicit");
    }

    @Test
    void insufficient_points_error_calculates_shortfall() {
        InsufficientPointsError err = new InsufficientPointsError(2_000, 5_000);
        assertThat(err.code()).isEqualTo("INSUFFICIENT_POINTS");
        assertThat(err.currentBalance()).isEqualTo(2_000);
        assertThat(err.requiredPoints()).isEqualTo(5_000);
        assertThat(err.shortfall()).isEqualTo(3_000);
    }

    @Test
    void payment_declined_error_carries_retry_flag() {
        PaymentDeclinedError err = new PaymentDeclinedError("declined", true);
        assertThat(err.code()).isEqualTo("PAYMENT_DECLINED");
        assertThat(err.retryWithNewCard()).isTrue();
    }

    @Test
    void already_enrolled_error_includes_existing_number() {
        AlreadyEnrolledError err = new AlreadyEnrolledError("LUX0001234567");
        assertThat(err.code()).isEqualTo("ALREADY_ENROLLED");
        assertThat(err.existingLoyaltyNumber()).isEqualTo("LUX0001234567");
        assertThat(err.message()).contains("LUX0001234567");
    }

    @Test
    void slot_unavailable_error_passes_through_alternatives() {
        SlotUnavailableError err = new SlotUnavailableError("slot-x",
                List.of("slot-y", "slot-z"));
        assertThat(err.code()).isEqualTo("SLOT_UNAVAILABLE");
        assertThat(err.suggestedAlternativeTokens()).containsExactly("slot-y", "slot-z");
    }

    @Test
    void slot_unavailable_error_handles_null_alternatives() {
        SlotUnavailableError err = new SlotUnavailableError("slot-x", null);
        assertThat(err.suggestedAlternativeTokens()).isEmpty();
    }
}
