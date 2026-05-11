package com.luxe.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimitStoreTest {

    @Test
    void allows_up_to_capacity_then_blocks() {
        // Tight bucket: 3 tokens / 60 seconds.
        var store = new InMemoryRateLimitStore(3, 60);
        assertThat(store.tryConsume("u1")).isTrue();
        assertThat(store.tryConsume("u1")).isTrue();
        assertThat(store.tryConsume("u1")).isTrue();
        assertThat(store.tryConsume("u1"))
                .as("fourth request must be denied — bucket exhausted")
                .isFalse();
    }

    @Test
    void buckets_are_isolated_by_key() {
        var store = new InMemoryRateLimitStore(1, 60);
        assertThat(store.tryConsume("user:a")).isTrue();
        assertThat(store.tryConsume("user:a")).isFalse();
        assertThat(store.tryConsume("user:b"))
                .as("different bucket key must have its own budget")
                .isTrue();
    }

    @Test
    void capacity_and_window_are_exposed_for_diagnostics() {
        var store = new InMemoryRateLimitStore(120, 60);
        assertThat(store.capacity()).isEqualTo(120);
        assertThat(store.refillDuration().getSeconds()).isEqualTo(60);
    }
}
