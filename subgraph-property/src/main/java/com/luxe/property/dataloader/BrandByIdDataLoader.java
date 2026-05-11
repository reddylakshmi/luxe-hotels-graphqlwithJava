package com.luxe.property.dataloader;

import com.luxe.property.datasource.PropertyDataSource;
import com.luxe.property.schema.types.Brand;
import com.netflix.graphql.dgs.DgsDataLoader;
import org.dataloader.MappedBatchLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Batches the {@code Hotel.brand} resolver. When a query asks for the
 * brand on N hotels, GraphQL would normally call the per-hotel
 * resolver N times — one {@code SELECT * FROM brand WHERE id = ?}
 * each on a real backend. The DataLoader collects all N brand ids
 * for the current request, dispatches <em>one</em> batched call
 * ({@link PropertyDataSource#getBrandsByIds(Set)}, equivalent to
 * {@code SELECT * FROM brand WHERE id IN (...)}), and hands each
 * resolver its slice back via a {@link CompletableFuture}.
 *
 * <p>DGS auto-discovers this bean via {@link DgsDataLoader} and
 * binds it to a request-scoped {@code DataLoader} instance, so two
 * concurrent GraphQL requests get isolated batching windows.
 *
 * <p>Why {@link MappedBatchLoader} instead of {@code BatchLoader}: a
 * mapped loader lets us return only the keys we actually found
 * without disturbing the order — important because the data source
 * might omit ids that don't exist, and dataloader-java is happy to
 * fill the missing slots with {@code null}.
 *
 * <p>Why an executor: the batch function runs on whatever thread
 * fires the dispatch. A separate single-thread executor keeps it off
 * the request-handling thread, which is well-mannered when the real
 * data source becomes blocking I/O.
 */
@DgsDataLoader(name = BrandByIdDataLoader.NAME, maxBatchSize = 100)
public class BrandByIdDataLoader implements MappedBatchLoader<String, Brand> {

    public static final String NAME = "brandById";

    private final PropertyDataSource dataSource;
    private final Executor batchExecutor;

    public BrandByIdDataLoader(PropertyDataSource dataSource) {
        this.dataSource = dataSource;
        this.batchExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "brand-batch-loader");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public CompletableFuture<Map<String, Brand>> load(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }
        return CompletableFuture.supplyAsync(() -> dataSource.getBrandsByIds(ids), batchExecutor);
    }
}
