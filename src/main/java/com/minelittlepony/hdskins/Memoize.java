package com.minelittlepony.hdskins;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongPredicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public interface Memoize<T> extends Supplier<T> {

    default void expireNow() {}

    static <T> Memoize<T> basic(Supplier<T> supplier) {
        return new Memoize<>() {
            @Nullable
            private Optional<T> value;

            @Nullable
            @Override
            public T get() {
                if (value == null) {
                    value = Optional.ofNullable(supplier.get());
                }
                return value.orElse(null);
            }

            public void expireNow() {
                value = null;
            }
        };
    }

    static <T> Memoize<T> nonExpiring(Supplier<T> supplier) {
        return supplier::get;
    }

    static <T> Memoize<T> withForcedExpiration(Supplier<T> supplier, AtomicLong timestamp) {
        return new Memoize<>() {
            private long lastTimestamp = timestamp.get();
            private Supplier<T> value = Suppliers.memoizeWithExpiration(supplier::get, 1, TimeUnit.SECONDS)::get;
            @Override
            public T get() {
                long time = timestamp.get();
                if (time != lastTimestamp) {
                    expireNow();
                }
                return value.get();
            }

            @Override
            public void expireNow() {
                value = Suppliers.memoizeWithExpiration(supplier::get, 1, TimeUnit.SECONDS)::get;
            }
        };
    }

    static <T> Memoize<T> withExpiration(Supplier<T> supplier) {
        return new Memoize<>() {
            private Supplier<T> value = Suppliers.memoizeWithExpiration(supplier::get, 1, TimeUnit.SECONDS)::get;
            @Override
            public T get() {
                return value.get();
            }

            @Override
            public void expireNow() {
                value = Suppliers.memoizeWithExpiration(supplier::get, 1, TimeUnit.SECONDS)::get;
            }
        };
    }

    static <T> Memoize<T> withExpirationPredicate(Supplier<T> supplier, LongPredicate expirationPredicate) {
        return new Memoize<>() {
            private long cacheTime = System.currentTimeMillis();
            @Nullable
            private final Memoize<T> basic = basic(supplier);
            @Nullable
            @Override
            public T get() {
                if (expirationPredicate.test(cacheTime)) {
                    expireNow();
                }
                return basic.get();
            }

            @Override
            public void expireNow() {
                cacheTime = System.currentTimeMillis();
                basic.expireNow();
            }
        };
    }

    static <K, V> LoadingCache<K, CompletableFuture<V>> createAsyncLoadingCache(long retentionPeriod, Function<K, CompletableFuture<V>> loadFunction) {
        return CacheBuilder.newBuilder()
            .expireAfterAccess(retentionPeriod, TimeUnit.SECONDS)
            .<K, CompletableFuture<V>>removalListener(entry -> entry.getValue().cancel(false))
            .build(CacheLoader.<K, CompletableFuture<V>>from(loadFunction));
    }
}
