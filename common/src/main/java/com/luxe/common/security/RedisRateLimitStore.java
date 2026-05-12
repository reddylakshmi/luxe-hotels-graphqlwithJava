package com.luxe.common.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Redis-backed {@link RateLimitStore} for horizontal-scale deployments.
 * Activated by setting {@code luxe.security.rate-limit.backend=redis}
 * — the default is {@code memory}, which keeps the existing
 * {@link InMemoryRateLimitStore} and avoids any Redis dependency at
 * runtime.
 *
 * <p>Wire-shape matches production: a Lettuce client connects to any
 * Redis (local docker-compose for dev, ElastiCache / Memorystore /
 * Azure Cache for prod). Bucket4j-Lettuce's CAS-based ProxyManager
 * implements the same token-bucket semantics as the in-memory store,
 * but the bucket state lives in Redis so every subgraph replica
 * agrees on the count.
 *
 * <p>At 30+ brands / 9000+ hotels / 100+ countries, the in-memory
 * store fails closed: scale a subgraph horizontally and each replica
 * gets its own private buckets, multiplying the effective limit by N.
 * This store fixes that by centralising bucket state.
 *
 * <p>Config:
 * <pre>
 * luxe.security.rate-limit.backend: redis        # opt-in
 * spring.data.redis.host: localhost              # standard Spring keys
 * spring.data.redis.port: 6379
 * </pre>
 *
 * <p>For Redis Cluster: swap RedisClient → RedisClusterClient and
 * LettuceBasedProxyManager → LettuceBasedProxyManager.builderFor(
 * clusterConnection); Bucket4j-Lettuce has a cluster-aware variant.
 */
@Component
@ConditionalOnProperty(prefix = "luxe.security.rate-limit", name = "backend",
        havingValue = "redis")
public class RedisRateLimitStore implements RateLimitStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitStore.class);
    private static final String KEY_PREFIX = "rl:";

    private final long capacity;
    private final Duration refillDuration;
    private final RedisClient client;
    private final StatefulRedisConnection<byte[], byte[]> connection;
    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final BucketConfiguration bucketConfig;

    public RedisRateLimitStore(
            @Value("${luxe.security.rate-limit.capacity:120}") long capacity,
            @Value("${luxe.security.rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {

        this.capacity = capacity;
        this.refillDuration = Duration.ofSeconds(windowSeconds);

        // Standalone Lettuce client — use RedisClusterClient + the
        // cluster builder for Redis Cluster deployments.
        this.client = RedisClient.create(String.format("redis://%s:%d", host, port));
        // Bucket4j-Lettuce's ProxyManager keys on the codec's K type;
        // using ByteArrayCodec for both K and V means the proxy
        // manager binds to byte[] keys, which we own here.
        this.connection = client.connect(ByteArrayCodec.INSTANCE);

        // Expire idle bucket keys ~1 minute past the refill window so
        // Redis doesn't accumulate dead buckets when traffic from a key
        // dries up. Active buckets keep getting touched and re-extended.
        ExpirationAfterWriteStrategy expiry =
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                        refillDuration.plusMinutes(1));

        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(expiry)
                .withClientSideConfig(ClientSideConfig.getDefault())
                .build();

        this.bucketConfig = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillDuration)
                        .build())
                .build();

        log.info("RedisRateLimitStore connected to {}:{} (capacity={}, window={}s)",
                host, port, capacity, windowSeconds);
    }

    @Override
    public boolean tryConsume(String key) {
        byte[] redisKey = (KEY_PREFIX + key).getBytes(StandardCharsets.UTF_8);
        BucketProxy bucket = proxyManager.builder().build(redisKey, () -> bucketConfig);
        return bucket.tryConsume(1);
    }

    @PreDestroy
    void shutdown() {
        try {
            if (connection != null) connection.close();
        } finally {
            if (client != null) client.shutdown();
        }
    }

    public long capacity() { return capacity; }
    public Duration refillDuration() { return refillDuration; }
}
