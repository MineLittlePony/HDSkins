package com.minelittlepony.hdskins.client.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

    public CompletableFuture<Identifier> loadAsync(Identifier imageId) {
        return CompletableFuture.supplyAsync(() -> {
            return getImage(imageId)
                    .flatMap(image -> Optional.ofNullable(HDPlayerSkinTexture.filterPlayerSkins(image))
                            .filter(i -> i != null && i != image)
                    );
        }, executor).thenApplyAsync(updated -> {
            return updated.map(image -> {
                Identifier convertedId = new Identifier(imageId.getNamespace() + "-converted", imageId.getPath());
                mc.getTextureManager().registerTexture(convertedId, new NativeImageBackedTexture(image));
                return convertedId;
            }).orElse(imageId);
        }, mc).exceptionally(t -> {
            HDSkins.LOGGER.warn("Errored while processing {}. Using original.", imageId, t);
            return imageId;
        });
    }

    @Nullable
    private Optional<NativeImage> getImage(Identifier res) {
        return mc.getResourceManager().getResource(res).map(resource -> {
            try (InputStream in = resource.getInputStream()) {
                return NativeImage.read(in);
            } catch (IOException e) {
                HDSkins.LOGGER.warn("Errored while reading image file ({}): {}.", res, e);
            }
            return null;
        });
    }
}
