package com.luxe.common.scalar;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void of_double_formats_to_two_decimals() {
        Money m = Money.of(1234.5, "USD");
        assertThat(m.amount()).isEqualTo("1234.50");
        assertThat(m.currency()).isEqualTo("USD");
    }

    @Test
    void of_double_handles_zero() {
        assertThat(Money.of(0.0, "EUR").amount()).isEqualTo("0.00");
    }

    @Test
    void of_double_handles_negative() {
        assertThat(Money.of(-99.99, "GBP").amount()).isEqualTo("-99.99");
    }

    @Test
    void of_bigdecimal_rounds_half_up_to_two_places() {
        Money m = Money.of(new BigDecimal("123.4567"), "JPY");
        assertThat(m.amount()).isEqualTo("123.46");
    }

    @Test
    void of_bigdecimal_pads_to_two_places() {
        Money m = Money.of(new BigDecimal("100"), "USD");
        assertThat(m.amount()).isEqualTo("100.00");
    }

    @Test
    void from_string_round_trips() {
        Money m = Money.fromString("450.00 USD");
        assertThat(m.amount()).isEqualTo("450.00");
        assertThat(m.currency()).isEqualTo("USD");
    }

    @Test
    void from_string_rejects_malformed_input() {
        assertThatThrownBy(() -> Money.fromString("just-text"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.fromString("100 USD EUR"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void amount_as_big_decimal_preserves_precision() {
        BigDecimal bd = Money.of(99.99, "USD").amountAsBigDecimal();
        assertThat(bd).isEqualByComparingTo("99.99");
    }

    @Test
    void add_sums_same_currency() {
        Money sum = Money.of(100.0, "USD").add(Money.of(50.0, "USD"));
        assertThat(sum.amount()).isEqualTo("150.00");
        assertThat(sum.currency()).isEqualTo("USD");
    }

    @Test
    void add_rejects_mixed_currencies() {
        assertThatThrownBy(() -> Money.of(100.0, "USD").add(Money.of(50.0, "EUR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
    }

    @Test
    void multiply_scales_amount() {
        Money tripled = Money.of(50.0, "USD").multiply(3.0);
        assertThat(tripled.amount()).isEqualTo("150.00");
    }

    @Test
    void multiply_handles_fractional_factor() {
        Money halved = Money.of(100.0, "USD").multiply(0.5);
        assertThat(halved.amount()).isEqualTo("50.00");
    }

    @Test
    void toString_uses_amount_space_currency() {
        assertThat(Money.of(100.0, "USD").toString()).isEqualTo("100.00 USD");
    }

    @Test
    void records_are_value_equal_when_amount_and_currency_match() {
        assertThat(Money.of(100.0, "USD")).isEqualTo(Money.of(100.0, "USD"));
        assertThat(Money.of(100.0, "USD")).isNotEqualTo(Money.of(100.0, "EUR"));
        assertThat(Money.of(100.0, "USD")).isNotEqualTo(Money.of(100.01, "USD"));
    }
}
