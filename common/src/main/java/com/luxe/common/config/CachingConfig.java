package com.luxe.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * In-process catalog cache (Tier 1 of the multi-layer strategy).
 *
 * <p>Targets read-heavy, low-churn data only — brands, hotel
 * descriptions, featured-hotel rolls, and the specialRates catalogue.
 * Availability and pricing are intentionally <em>not</em> cached here;
 * inventory drift is more expensive than the latency we'd save.
 *
 * <p>Why one config per cache (not a default + customizer):
 * specialRates is a 5-entry constant that wants a long TTL, whereas
 * brand / hotel reads need a shorter TTL so a CMS edit reaches users
 * within a few minutes. Encoding the TTL per cache keeps the contract
 * explicit at the @Cacheable call site.
 *
 * <p>{@link ConditionalOnMissingBean} lets a real deployment swap in
 * a Redis-backed {@link CacheManager} without touching annotations —
 * the eventual Tier 2 move when subgraphs scale horizontally.
 */
@Configuration
@EnableCaching
public class CachingConfig {

    public static final String CATALOG_BRAND       = "catalog.brand";
    public static final String CATALOG_BRANDS_LIST = "catalog.brandsList";
    public static final String CATALOG_FEATURED    = "catalog.featuredHotels";
    public static final String PRICING_SPECIAL_RATES = "pricing.specialRates";

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager catalogCacheManager() {
        SimpleCacheManager mgr = new SimpleCacheManager();
        mgr.setCaches(List.of(
                build(CATALOG_BRAND,        1_000, 5, TimeUnit.MINUTES),
                build(CATALOG_BRANDS_LIST,    100, 5, TimeUnit.MINUTES),
                build(CATALOG_FEATURED,       200, 5, TimeUnit.MINUTES),
                // 5-entry hard-coded product catalogue — long TTL is fine.
                build(PRICING_SPECIAL_RATES,    8, 1, TimeUnit.HOURS)
        ));
        return mgr;
    }

    private static CaffeineCache build(String name, long maxEntries, long ttl, TimeUnit unit) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maxEntries)
                .expireAfterWrite(ttl, unit)
                .recordStats()
                .build());
    }
}
