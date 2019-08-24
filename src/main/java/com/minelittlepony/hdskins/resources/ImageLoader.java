package com.minelittlepony.hdskins.resources;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import com.minelittlepony.hdskins.resources.texture.ImageBufferDownloadHD;
import com.minelittlepony.hdskins.util.CallableFutures;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
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
        return CallableFutures.asyncFailableFuture(() -> {
            NativeImage image = getImage(original);

            final NativeImage updated = new ImageBufferDownloadHD().filterImage(image);

            if (updated == null || updated == image) {
                return original; // don't load a new image
            }

            return mc.executeFuture(() -> {
                Identifier conv = new Identifier(original.getNamespace() + "-converted", original.getPath());

                if (mc.getTextureManager().registerTexture(conv, new NativeImageBackedTexture(updated))) {
                    return conv;
                }

                return original;
            }).get();
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
