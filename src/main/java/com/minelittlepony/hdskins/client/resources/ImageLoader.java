package com.minelittlepony.hdskins.client.resources;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

public class ImageLoader {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public void stop() {
        executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();
    }

    @SuppressWarnings("resource")
    public CompletableFuture<Identifier> loadAsync(Identifier original) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                NativeImage image = getImage(original);

                final NativeImage updated = HDPlayerSkinTexture.filterPlayerSkins(image);

                if (updated == null || updated == image) {
                    return original; // don't load a new image
                }

                return CompletableFuture.supplyAsync(() -> {
                    Identifier conv = new Identifier(original.getNamespace() + "-converted", original.getPath());

                    if (mc.getTextureManager().registerTexture(conv, new NativeImageBackedTexture(updated))) {
                        return conv;
                    }

                    return original;
                }, mc).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Nullable
    private NativeImage getImage(Identifier res) {
        try (InputStream in = mc.getResourceManager().getResource(res).getInputStream()) {
            return NativeImage.read(in);
        } catch (IOException e) {
            return null;
        }
    }
}
