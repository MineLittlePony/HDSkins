package com.minelittlepony.hdskins.client.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.HDSkins;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class TextureLoader {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    /**
     * Schedule texture loading on the main thread.
     * @param textureLocation
     * @param texture
     */
    public static <T extends AbstractTexture> T loadTexture(final Identifier textureLocation, final T texture) {
        CLIENT.execute(() -> {
            RenderSystem.recordRenderCall(() -> {
                CLIENT.getTextureManager().registerTexture(textureLocation, texture);
            });
        });
        return texture;
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private final BiFunction<NativeImage, Exclusion, NativeImage> filter;

    private final String id;

    public TextureLoader(String id, BiFunction<NativeImage, Exclusion, NativeImage> filter) {
        this.id = id;
        this.filter = filter;
    }

    public void stop() {
        executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();
    }

    public CompletableFuture<Identifier> loadAsync(Identifier imageId) {
        return loadAsync(imageId, Exclusion.NULL);
    }

    public CompletableFuture<Identifier> loadAsync(Identifier imageId, Exclusion exclusion) {
        return CompletableFuture.supplyAsync(() -> getImage(imageId), executor)
        .thenApplyAsync(loaded -> loaded
                .flatMap(image -> Optional.ofNullable(filter.apply(image, exclusion))
                .filter(i -> i != null && i != image)), CLIENT)
        .thenApplyAsync(updated -> {
            return updated.map(image -> {
                Identifier convertedId = new Identifier(imageId.getNamespace(), "dynamic/" + id + "/" + imageId.getPath());
                CLIENT.getTextureManager().registerTexture(convertedId, new NativeImageBackedTexture(image));
                return convertedId;
            }).orElse(imageId);
        }, CLIENT).exceptionally(t -> {
            HDSkins.LOGGER.warn("Errored while processing {}. Using original.", imageId, t);
            return imageId;
        });
    }

    @Nullable
    private Optional<NativeImage> getImage(Identifier res) {

        AbstractTexture tex = CLIENT.getTextureManager().getOrDefault(res, (AbstractTexture)null);

        if (tex instanceof NativeImageBackedTexture nat) {
            return Optional.ofNullable(nat.getImage());
        }

        return CLIENT.getResourceManager().getResource(res).map(resource -> {
            try (InputStream in = resource.getInputStream()) {
                return NativeImage.read(in);
            } catch (IOException e) {
                HDSkins.LOGGER.warn("Errored while reading image file ({}): {}.", res, e);
            }
            return null;
        });
    }

    public interface Exclusion {
        Exclusion NULL = (x, y) -> false;

        boolean includes(int x, int y);
    }
}
