package com.minelittlepony.hdskins.client.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.HDSkins;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class TextureLoader {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Schedule texture loading on the main thread.
     * @param textureLocation
     * @param texture
     */
    public static void loadTexture(final Identifier textureLocation, final AbstractTexture texture) {
        mc.execute(() -> {
            RenderSystem.recordRenderCall(() -> {
                mc.getTextureManager().registerTexture(textureLocation, texture);
            });
        });
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public void stop() {
        executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();
    }

    public CompletableFuture<Identifier> loadAsync(Identifier original) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                @Nullable
                NativeImage image = getImage(original);

                @Nullable
                final NativeImage updated = HDPlayerSkinTexture.filterPlayerSkins(image);

                if (updated == null || updated == image) {
                    return original; // don't load a new image
                }

                Identifier conv = new Identifier(original.getNamespace() + "-converted", original.getPath());

                return CompletableFuture.supplyAsync(() -> {
                    mc.getTextureManager().registerTexture(conv, new NativeImageBackedTexture(updated));
                    return conv;
                }, mc).get();
            } catch (InterruptedException | ExecutionException e) {
                HDSkins.LOGGER.warn("Errored while processing {}. Using original.", original, e);

                return original;
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
