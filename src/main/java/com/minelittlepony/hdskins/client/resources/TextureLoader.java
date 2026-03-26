package com.minelittlepony.hdskins.client.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.HDSkins;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class TextureLoader {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    public static CompletableFuture<Identifier> uploadTexture(Identifier textureId, NativeImage image) {
        return CompletableFuture.supplyAsync(() -> {
            CLIENT.getTextureManager().register(textureId, new DynamicTexture(textureId::toString, image));
            return textureId;
        }, CLIENT);
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
        return getImage(imageId)
            .thenApplyAsync(loaded -> loaded
                    .flatMap(image -> Optional.ofNullable(filter.apply(image, exclusion))
                    .filter(i -> i != null && i != image)), executor)
            .thenApplyAsync(updated -> {
                return updated.map(image -> {
                    Identifier convertedId = imageId.withPath(p -> "dynamic/" + id + "/" + p);
                    CLIENT.getTextureManager().register(convertedId, new DynamicTexture(convertedId::toString, image));
                    return convertedId;
                }).orElse(imageId);
            }, CLIENT).exceptionally(t -> {
                HDSkins.LOGGER.warn("Errored while processing {}. Using original.", imageId, t);
                return imageId;
            });
    }

    @Nullable
    private CompletableFuture<Optional<NativeImage>> getImage(Identifier res) {
        return CompletableFuture.<CompletableFuture<Optional<NativeImage>>>supplyAsync(() -> {
            if (CLIENT.getTextureManager().getTexture(res) instanceof DynamicTexture nat) {
                return CompletableFuture.completedFuture(Optional.ofNullable(nat.getPixels()));
            }

            return CLIENT.getResourceManager().getResource(res).map(resource -> {
                return CompletableFuture.<Optional<NativeImage>>supplyAsync(() -> {
                    try (InputStream in = resource.open()) {
                        return Optional.of(NativeImage.read(in));
                    } catch (IOException e) {
                        HDSkins.LOGGER.warn("Errored while reading image file ({}): {}.", res, e);
                    }
                    return Optional.empty();
                }, executor);
            }).orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));
        }, CLIENT).thenCompose(Function.identity());
    }

    public interface Exclusion {
        Exclusion NULL = (_, _) -> false;

        boolean includes(int x, int y);
    }
}
