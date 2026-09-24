package com.minelittlepony.hdskins;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.google.common.cache.LoadingCache;
import net.minecraft.util.Util;

public class BufferedCache<K, V> implements Function<K, CompletableFuture<V>> {
    private static final long MAX_EXECUTOR_DELAY = 10_000 /*10s*/;
    private static final long MIN_EXECUTOR_DELAY = 50 /*50ms*/;

    private final Executor executor = Util.nonCriticalIoPool();
    private Executor delayedExecutor;

    private final AtomicReference<@Nullable Batch<K, V>> activeBatch = new AtomicReference<>(null);

    private final LoadingCache<K, CompletableFuture<V>> cache;

    public BufferedCache(long tickDelay, Function<Collection<K>, Map<K, V>> loadFunction) {
        setLoadDelay(tickDelay);
        cache = Memoize.createAsyncLoadingCache(Memoize.DEFAULT_DURATION, k -> {
            return activeBatch.updateAndGet(previous -> {
                return previous == null || previous.hasBeenStarted() ? new Batch<K, V>(loadFunction, delayedExecutor) : previous;
            }).addAndGetFuture(k);
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

    private record Batch<K, V>(
            CompletableFuture<CompletableFuture<Map<K, V>>> future,
            Function<Collection<K>, Map<K, V>> loadFunction,
            Map<K, CompletableFuture<V>> values,
            Executor executor
    ) {
        public Batch(Function<Collection<K>, Map<K, V>> loadFunction, Executor executor) {
            this(new CompletableFuture<>(), loadFunction, new HashMap<>(), executor);
        }

        private Map<K, V> prepare() {
            synchronized (values()) {
                return loadFunction.apply(values().keySet());
            }
        }

        private Map<K, V> apply(Map<K, V> data) {
            data.forEach((key, value) -> {
                synchronized (values()) {
                    var f = values().get(key);
                    if (f != null && !f.isCancelled()) {
                        f.complete(value);
                    }
                }
            });
            return data;
        }

        public boolean hasBeenStarted() {
            return future().isDone();
        }

        public CompletableFuture<V> addAndGetFuture(K k) {
            synchronized (values()) {
                if (values.isEmpty()) {
                    CompletableFuture.runAsync(() -> {
                        future().complete(CompletableFuture.supplyAsync(this::prepare, Util.nonCriticalIoPool()).thenApply(this::apply));
                    }, executor);
                }
                return values().computeIfAbsent(k, _ -> new CompletableFuture<V>());
            }
        }
    }
}
