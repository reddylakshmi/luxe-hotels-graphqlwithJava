package com.luxe.property.dataloader;

import com.luxe.property.datasource.PropertyDataSource;
import com.luxe.property.schema.types.RoomType;
import com.netflix.graphql.dgs.DgsDataLoader;
import org.dataloader.MappedBatchLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Batches the {@code Hotel.roomTypes} resolver. A query like
 * {@code featuredHotels { roomTypes { id name } }} otherwise produces
 * one room-type query per hotel — same N+1 shape as {@code Hotel.brand}.
 *
 * <p>The batch function in the mock data source runs a single pass
 * over the room-type collection and groups by hotelId. On a real
 * backend this becomes one {@code SELECT ... WHERE hotel_id IN (...)
 * GROUP BY hotel_id} or one Redis MGET, depending on the storage
 * layer.
 *
 * <p>The map value type is {@code List<RoomType>} (not just
 * {@code RoomType}) because each hotel can own many room types — the
 * DataLoader contract still works fine when the value is a
 * collection; the framework treats it as one key → one (composite)
 * value.
 */
@DgsDataLoader(name = RoomTypesByHotelIdDataLoader.NAME, maxBatchSize = 100)
public class RoomTypesByHotelIdDataLoader
        implements MappedBatchLoader<String, List<RoomType>> {

    public static final String NAME = "roomTypesByHotelId";

    private final PropertyDataSource dataSource;
    private final Executor batchExecutor;

    public RoomTypesByHotelIdDataLoader(PropertyDataSource dataSource) {
        this.dataSource = dataSource;
        this.batchExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "room-type-batch-loader");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public CompletableFuture<Map<String, List<RoomType>>> load(Set<String> hotelIds) {
        if (hotelIds == null || hotelIds.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }
        return CompletableFuture.supplyAsync(
                () -> dataSource.getRoomTypesByHotelIds(hotelIds), batchExecutor);
    }
}
