package com.luxe.property.dataloader;

import com.luxe.property.datasource.PropertyDataSource;
import com.luxe.property.schema.types.Hotel;
import com.netflix.graphql.dgs.DgsDataLoader;
import org.dataloader.MappedBatchLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Batches hotel-by-id lookups. Two distinct callers use this:
 *
 * <ol>
 *   <li>Federation {@code _entities} — when a foreign subgraph
 *       returns a list of Hotel references and the router needs to
 *       hydrate them via the property subgraph, the DGS entity
 *       fetcher is invoked once per representation. Without
 *       batching, that's an N+1 across the whole request.</li>
 *   <li>Any future internal resolver that follows a {@code hotelId}
 *       reference (e.g. {@code Reservation.hotel} resolved within
 *       property if we ever inline that).</li>
 * </ol>
 *
 * Both paths converge on
 * {@link PropertyDataSource#getHotelsByIds(java.util.Set)} —
 * one batched call per request even when the same hotel id was
 * requested through multiple paths.
 */
@DgsDataLoader(name = HotelByIdDataLoader.NAME, maxBatchSize = 100)
public class HotelByIdDataLoader implements MappedBatchLoader<String, Hotel> {

    public static final String NAME = "hotelById";

    private final PropertyDataSource dataSource;
    private final Executor batchExecutor;

    public HotelByIdDataLoader(PropertyDataSource dataSource) {
        this.dataSource = dataSource;
        this.batchExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "hotel-batch-loader");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public CompletableFuture<Map<String, Hotel>> load(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }
        return CompletableFuture.supplyAsync(() -> dataSource.getHotelsByIds(ids), batchExecutor);
    }
}
