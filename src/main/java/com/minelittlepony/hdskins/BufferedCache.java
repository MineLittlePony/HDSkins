package com.minelittlepony.hdskins;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.google.common.cache.LoadingCache;

import net.minecraft.util.Util;

public class BufferedCache<K, V> implements Function<K, CompletableFuture<V>> {
    private final Executor executor = Util.getIoWorkerExecutor();
    private final Executor delayedExecutor = CompletableFuture.delayedExecutor(1, TimeUnit.MILLISECONDS, executor);

    private final AtomicReference<Function<K, CompletableFuture<V>>> activeBatch = new AtomicReference<>(null);

    private final LoadingCache<K, CompletableFuture<V>> cache;

    public BufferedCache(Function<Collection<K>, Map<K, V>> loadFunction) {
        cache = Memoize.createAsyncLoadingCache(15, k -> {
            return this.activeBatch.updateAndGet(previous -> {
                if (previous == null) {
                    Set<K> keys = new HashSet<>();
                    return new Batch<K, V>(CompletableFuture.supplyAsync(() -> {
                        this.activeBatch.set(null);
                        return loadFunction.apply(keys);
                    }, delayedExecutor), keys);
                }
                return previous;
            }).apply(k);
        });
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public CompletableFuture<V> apply(K k) {
        return cache.getUnchecked(k);
    }

    record Batch<K, V>(CompletableFuture<Map<K, V>> future, Set<K> collection) implements Function<K, CompletableFuture<V>> {
        @Override
        public CompletableFuture<V> apply(K k) {
            collection.add(k);
            return future().thenApply(results -> {
               return results.get(k);
            });
        }
    }
}
