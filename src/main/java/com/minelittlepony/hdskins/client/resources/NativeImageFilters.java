package com.minelittlepony.hdskins.client.resources;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.LoadingCache;
import com.minelittlepony.hdskins.Memoize;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

public interface NativeImageFilters {
    NativeImageFilters GREYSCALE = create("default_player_skin", color -> {
        int a = ColorHelper.getAlpha(color);
        float r = ColorHelper.getRed(color) / 255F;
        float g = ColorHelper.getGreen(color) / 255F;
        float b = ColorHelper.getBlue(color) / 255F;
        int brightness = (int)((0.2126F * r + 0.7152F * g + 0.0722F * b) * 255);
        return ColorHelper.getArgb(a, brightness, brightness, brightness);
    });
    NativeImageFilters REDUCE_ALPHA = create("default_player_skin_half_alpha", color -> {
        return ColorHelper.withAlpha(Math.min(ColorHelper.getAlpha(color), 0x20), color);
    });

    static NativeImageFilters create(String name, Int2IntFunction pixelTransformation) {
        final var loader = new TextureLoader(name, (image, exclusion) -> {
            NativeImage copy = new NativeImage(image.getFormat(), image.getWidth(), image.getHeight(), false);
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    if (exclusion.includes(x, y)) {
                        copy.setColorArgb(x, y, image.getColorArgb(x, y));
                    } else {
                        copy.setColorArgb(x, y, pixelTransformation.applyAsInt(image.getColorArgb(x, y)));
                    }
                }
            }
            image.close();

            return copy;
        });
        final LoadingCache<Pair<Identifier, TextureLoader.Exclusion>, CompletableFuture<Identifier>> cache = Memoize.createAsyncLoadingCache(15, pair -> loader.loadAsync(pair.left(), pair.right()));

        return (id, fallback, exclusion) -> {
            try {
                return cache.get(new Pair<>(id, exclusion)).getNow(fallback);
            } catch (ExecutionException ignored) { } finally { }
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
        return DefaultSkinHelper.getSkinTextures(UUID.nameUUIDFromBytes(randomBytes)).body().texturePath();
    }

    record Pair<A, B>(A left, B right) {}
}
