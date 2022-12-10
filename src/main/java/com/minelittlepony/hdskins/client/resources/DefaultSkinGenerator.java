package com.minelittlepony.hdskins.client.resources;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;

import com.google.common.cache.*;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

public class DefaultSkinGenerator {
    private static final TextureLoader LOADER = new TextureLoader("default_player_skin", (image, exclusion) -> {
        NativeImage copy = new NativeImage(image.getFormat(), image.getWidth(), image.getHeight(), false);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (exclusion.includes(x, y)) {
                    copy.setColor(x, y, image.getColor(x, y));
                } else {
                    int a = image.getOpacity(x, y);
                    float r = image.getRed(x, y) / 255F;
                    float g = image.getGreen(x, y) / 255F;
                    float b = image.getBlue(x, y) / 255F;
                    int brightness = (int)((0.2126F * r + 0.7152F * g + 0.0722F * b) * 255);
                    copy.setColor(x, y, NativeImage.packColor(a, brightness, brightness, brightness));
                }
            }
        }

        return copy;
    });
    private static final LoadingCache<Pair<Identifier, TextureLoader.Exclusion>, CompletableFuture<Identifier>> CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build(CacheLoader.from(pair -> LOADER.loadAsync(pair.getLeft(), pair.getRight())));

    public static Identifier generateGreyScale(Identifier id, Identifier fallback, TextureLoader.Exclusion exclusion) {
        try {
            return CACHE.get(new Pair<>(id, exclusion)).getNow(fallback);
        } catch (ExecutionException e) { }
        return fallback;
    }

    public static Identifier getCyclicDefaultTexture() {
        byte[] randomBytes = new byte[16];
        new Random(System.currentTimeMillis() / 1000).nextBytes(randomBytes);
        randomBytes[6]  &= 0x0f;  /* clear version        */
        randomBytes[6]  |= 0x40;  /* set to version 4     */
        randomBytes[8]  &= 0x3f;  /* clear variant        */
        randomBytes[8]  |= 0x80;  /* set to IETF variant  */
        return DefaultSkinHelper.getTexture(UUID.nameUUIDFromBytes(randomBytes));
    }
}
