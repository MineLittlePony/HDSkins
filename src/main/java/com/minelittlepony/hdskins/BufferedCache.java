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
    private static final long MAX_EXECUTOR_DELAY = 10_000 /*10s*/;
    private static final long MIN_EXECUTOR_DELAY = 50 /*50ms*/;

    private final Executor executor = Util.nonCriticalIoPool();
    private Executor delayedExecutor;

    private final AtomicReference<Function<K, CompletableFuture<V>>> activeBatch = new AtomicReference<>(null);

    private final LoadingCache<K, CompletableFuture<V>> cache;

    public BufferedCache(long tickDelay, Function<Collection<K>, Map<K, V>> loadFunction) {
        setLoadDelay(tickDelay);
        cache = Memoize.createAsyncLoadingCache(Memoize.DEFAULT_DURATION, k -> {
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

    public void setLoadDelay(long ticks) {
        delayedExecutor = CompletableFuture.delayedExecutor(Math.clamp(ticks * MIN_EXECUTOR_DELAY, MIN_EXECUTOR_DELAY, MAX_EXECUTOR_DELAY), TimeUnit.MILLISECONDS, executor);
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
