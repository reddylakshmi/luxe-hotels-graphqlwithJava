package com.luxe.property.resolver;

import com.luxe.property.datasource.PropertyMockDataSource;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks in the DataLoader contract for property: a multi-hotel query
 * that selects a per-hotel field (brand, roomTypes) must dispatch
 * <em>one</em> batched DataSource call, not N single-key calls.
 *
 * <p>Strategy: read the test-only call counters on
 * {@link PropertyMockDataSource}. They increment <strong>once per
 * batched call</strong> regardless of how many keys are in the
 * batch — so the assertion is exact, not statistical.
 *
 * <p>If a future refactor accidentally re-introduces the N+1 path
 * (e.g. by reverting a resolver to a synchronous
 * {@code getBrandById}), this test fails immediately because the
 * batched counter stays at 0 while the per-call counter would climb.
 */
@SpringBootTest
class DataLoaderBatchingTest {

    @Autowired DgsQueryExecutor dgs;
    @Autowired PropertyMockDataSource dataSource;

    @BeforeEach
    void resetCounters() {
        dataSource.brandBatchCalls.set(0);
        dataSource.hotelBatchCalls.set(0);
        dataSource.roomTypeBatchCalls.set(0);
    }

    @Test
    void featured_hotels_with_brand_uses_ONE_batched_brand_call() {
        // Without DataLoader: 9 hotels × 1 brand lookup each = 9 calls.
        // With DataLoader:    1 batched call for all 9 brand ids.
        var result = dgs.execute("""
                { featuredHotels(first: 9) { id name brand { id name tier } } }
                """);
        assertThat(result.getErrors()).isEmpty();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hotels =
                (List<Map<String, Object>>) ((Map<String, Object>) result.getData())
                        .get("featuredHotels");
        assertThat(hotels).hasSizeGreaterThanOrEqualTo(5)
                .as("seed must include enough featured hotels to make the batch meaningful");
        // Every hotel resolved a brand.
        assertThat(hotels).allSatisfy(h ->
                assertThat(((Map<?, ?>) h.get("brand")).get("name"))
                        .as("brand.name must populate for each hotel")
                        .isNotNull());
        assertThat(dataSource.brandBatchCalls.get())
                .as("DataLoader must collapse N brand lookups into one batched call")
                .isEqualTo(1);
    }

    @Test
    void featured_hotels_with_roomTypes_uses_ONE_batched_roomTypes_call() {
        var result = dgs.execute("""
                { featuredHotels(first: 6) { id roomTypes { id name } } }
                """);
        assertThat(result.getErrors()).isEmpty();
        assertThat(dataSource.roomTypeBatchCalls.get())
                .as("DataLoader must batch room-type lookups across all hotels")
                .isEqualTo(1);
    }

    @Test
    void federation_entities_for_multiple_hotels_uses_ONE_batched_hotel_call() {
        // The router fans an article's relatedHotels into N
        // _entities reps. The entity fetcher routes through the
        // hotelById DataLoader, so all N reps resolve in one batch.
        var result = dgs.execute("""
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on Hotel { id name }
                  }
                }
                """, Map.of("reps", List.of(
                        Map.of("__typename", "Hotel", "id", "prop-paris-001"),
                        Map.of("__typename", "Hotel", "id", "prop-tokyo-001"),
                        Map.of("__typename", "Hotel", "id", "prop-london-001"),
                        Map.of("__typename", "Hotel", "id", "prop-dubai-001"),
                        Map.of("__typename", "Hotel", "id", "prop-nyc-001"))));
        assertThat(result.getErrors()).isEmpty();
        assertThat(dataSource.hotelBatchCalls.get())
                .as("_entities federation lookups must batch through hotelById DataLoader")
                .isEqualTo(1);
    }

    @Test
    void single_hotel_query_still_works_through_the_loader() {
        // Edge case: a one-key request shouldn't degrade. The
        // DataLoader still dispatches one batched call (of size 1),
        // which is operationally identical to a direct lookup but
        // keeps the code path uniform.
        var result = dgs.execute(
                "{ featuredHotels(first: 1) { brand { name } } }");
        assertThat(result.getErrors()).isEmpty();
        assertThat(dataSource.brandBatchCalls.get()).isEqualTo(1);
    }
}
