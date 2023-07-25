package com.minelittlepony.hdskins.client.resources;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

public interface NativeImageFilters {
    NativeImageFilters GREYSCALE = create("default_player_skin", color -> {
        int a = NativeImage.getAlpha(color);
        float r = NativeImage.getRed(color) / 255F;
        float g = NativeImage.getGreen(color) / 255F;
        float b = NativeImage.getBlue(color) / 255F;
        int brightness = (int)((0.2126F * r + 0.7152F * g + 0.0722F * b) * 255);
        return NativeImage.packColor(a, brightness, brightness, brightness);
    });
    NativeImageFilters REDUCE_ALPHA = create("default_player_skin_half_alpha", color -> {
        int a = Math.min(NativeImage.getAlpha(color), 0x30);
        return (color & 0x00FFFFFF) | (a << 24);
    });

    static NativeImageFilters create(String name, Int2IntFunction pixelTransformation) {
        final var loader = new TextureLoader(name, (image, exclusion) -> {
            NativeImage copy = new NativeImage(image.getFormat(), image.getWidth(), image.getHeight(), false);
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    if (exclusion.includes(x, y)) {
                        copy.setColor(x, y, image.getColor(x, y));
                    } else {
                        copy.setColor(x, y, pixelTransformation.applyAsInt(image.getColor(x, y)));
                    }
                }
            }

            return copy;
        });
        final LoadingCache<Pair<Identifier, TextureLoader.Exclusion>, CompletableFuture<Identifier>> cache = CacheBuilder.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build(CacheLoader.from(pair -> loader.loadAsync(pair.left(), pair.right())));

        return (id, fallback, exclusion) -> {
            try {
                return cache.get(new Pair<>(id, exclusion)).getNow(fallback);
            } catch (ExecutionException e) { } finally {
            }
            return fallback;
        };
    }

    Identifier load(Identifier id, Identifier fallback, TextureLoader.Exclusion exclusion);

    static Identifier getCyclicDefaultTexture() {
        byte[] randomBytes = new byte[16];
        new Random(System.currentTimeMillis() / 1000).nextBytes(randomBytes);
        randomBytes[6]  &= 0x0f;  /* clear version        */
        randomBytes[6]  |= 0x40;  /* set to version 4     */
        randomBytes[8]  &= 0x3f;  /* clear variant        */
        randomBytes[8]  |= 0x80;  /* set to IETF variant  */
        return DefaultSkinHelper.getTexture(UUID.nameUUIDFromBytes(randomBytes));
    }

    record Pair<A, B>(A left, B right) {}
}
