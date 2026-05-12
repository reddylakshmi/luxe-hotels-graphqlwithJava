package com.luxe.common.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link RateLimitStore} backed by an in-memory
 * {@link ConcurrentHashMap} of Bucket4j {@link Bucket}s. Each bucket
 * holds {@link #capacity} tokens and refills at {@link #capacity}
 * tokens / {@link #refillDuration}.
 *
 * <p>Defaults (env-tunable):
 * <pre>
 * luxe.security.rate-limit.capacity: 120        # 120 requests per window
 * luxe.security.rate-limit.window-seconds: 60   # 60 seconds
 * </pre>
 *
 * <p>That's 2 RPS sustained per user — generous for a hotel UI,
 * tight enough to embarrass a scraper. Per-anonymous-IP limits run
 * tighter (see {@code RateLimitFilter}) so an unauthenticated burst
 * can't drain a server.
 *
 * <p>This class is the only place that knows about Bucket4j local
 * buckets. The Redis-backed counterpart is {@link RedisRateLimitStore};
 * pick between them by setting {@code luxe.security.rate-limit.backend}
 * to either {@code memory} (default) or {@code redis}.
 */
@Configuration
@ConditionalOnProperty(prefix = "luxe.security.rate-limit", name = "backend",
        havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimitStore implements RateLimitStore {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final Duration refillDuration;

    public InMemoryRateLimitStore(
            @Value("${luxe.security.rate-limit.capacity:120}") long capacity,
            @Value("${luxe.security.rate-limit.window-seconds:60}") long windowSeconds) {
        this.capacity = capacity;
        this.refillDuration = Duration.ofSeconds(windowSeconds);
    }

    @org.springframework.context.annotation.Bean
    @ConditionalOnMissingBean(RateLimitStore.class)
    public RateLimitStore rateLimitStore() {
        return this;
    }

    @Override
    public boolean tryConsume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, this::newBucket);
        return bucket.tryConsume(1);
    }

    private Bucket newBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillDuration)
                        .build())
                .build();
    }

    public long capacity() { return capacity; }
    public Duration refillDuration() { return refillDuration; }
}
