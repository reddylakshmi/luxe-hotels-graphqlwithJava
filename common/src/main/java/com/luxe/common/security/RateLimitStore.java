package com.luxe.common.security;

/**
 * Abstraction over the rate-limit backend. The default Spring bean
 * is {@link InMemoryRateLimitStore} — fast, no infra, fine for local
 * dev + the in-memory mock world this repo runs in.
 *
 * <p>Production swaps to a Redis-backed implementation (Bucket4j-Redis
 * via Lettuce/Redisson) by registering a bean that implements this
 * interface; no other code changes. The router-side bucket coverage
 * (per-IP) plus the subgraph-side bucket coverage (per-authenticated-
 * user) are both expressed against the same interface, so swap is one
 * file.
 *
 * <h3>Contract</h3>
 * Implementations return {@code true} when the caller should be
 * allowed to proceed and {@code false} when the bucket is exhausted.
 * No exceptions for the "rate-limited" case — that's the happy path
 * for the filter to decide whether to write a 429.
 */
public interface RateLimitStore {

    /**
     * Try to consume one token from the bucket identified by
     * {@code key}. Returns {@code true} if the request fits within
     * the configured rate, {@code false} if it would exceed it.
     */
    boolean tryConsume(String key);
}
