package com.luxe.common.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opentest4j.TestAbortedException;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hermetic test for {@link RedisRateLimitStore}. Spins up an
 * embedded Redis on a free port, exercises the same contract as
 * {@link InMemoryRateLimitStoreTest}, and shuts the server down.
 *
 * <p>Embedded Redis is <em>test-scope only</em> and a dev convenience
 * — production CI should run this same test class against a
 * Testcontainers-managed real Redis. The bundled binary needs
 * {@code openssl@3} on Apple Silicon; when it's not available the
 * test class skips rather than failing red so contributors without
 * the brew formula can still run {@code mvn test} cleanly.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisRateLimitStoreTest {

    private RedisServer server;
    private int port;
    private String skipReason;

    @BeforeAll
    void startRedis() throws IOException {
        port = findFreePort();
        try {
            server = new RedisServer(port);
            server.start();
        } catch (IOException | RuntimeException e) {
            // Most common cause on macOS dev boxes: missing
            // /opt/homebrew/opt/openssl@3/lib/libssl.3.dylib. Skip
            // loudly instead of failing the suite — CI runs this
            // against a real Redis (Testcontainers) anyway.
            skipReason = "Embedded Redis could not start: " + e.getMessage();
            server = null;
        }
    }

    @BeforeEach
    void skipIfNoRedis() {
        if (skipReason != null) throw new TestAbortedException(skipReason);
    }

    @AfterAll
    void stopRedis() throws IOException {
        if (server != null) server.stop();
    }

    @Test
    void allows_up_to_capacity_then_blocks() {
        var store = new RedisRateLimitStore(3, 60, "localhost", port);
        try {
            assertThat(store.tryConsume("u1")).isTrue();
            assertThat(store.tryConsume("u1")).isTrue();
            assertThat(store.tryConsume("u1")).isTrue();
            assertThat(store.tryConsume("u1"))
                    .as("fourth request must be denied — bucket exhausted")
                    .isFalse();
        } finally {
            store.shutdown();
        }
    }

    @Test
    void buckets_are_isolated_by_key() {
        var store = new RedisRateLimitStore(1, 60, "localhost", port);
        try {
            assertThat(store.tryConsume("user:isolated-a")).isTrue();
            assertThat(store.tryConsume("user:isolated-a")).isFalse();
            assertThat(store.tryConsume("user:isolated-b"))
                    .as("different bucket key must have its own budget")
                    .isTrue();
        } finally {
            store.shutdown();
        }
    }

    @Test
    void two_store_instances_share_redis_state() {
        // The whole point of moving to Redis: subgraph replicas
        // share buckets. Two stores pointing at the same Redis must
        // see the same counter.
        var a = new RedisRateLimitStore(2, 60, "localhost", port);
        var b = new RedisRateLimitStore(2, 60, "localhost", port);
        try {
            assertThat(a.tryConsume("user:shared")).isTrue();
            assertThat(b.tryConsume("user:shared")).isTrue();
            assertThat(a.tryConsume("user:shared"))
                    .as("third consume across replicas must hit the shared bucket limit")
                    .isFalse();
        } finally {
            a.shutdown();
            b.shutdown();
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
