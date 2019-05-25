package com.minelittlepony.hdskins.resources;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import com.minelittlepony.hdskins.resources.texture.ImageBufferDownloadHD;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javax.annotation.Nullable;

public class ImageLoader implements Supplier<Identifier> {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final Identifier original;

    public ImageLoader(Identifier loc) {
        this.original = loc;
    }

    @Override
    @Nullable
    public Identifier get() {
        NativeImage image = getImage(original);

        final NativeImage updated = new ImageBufferDownloadHD().parseUserSkin(image);

        if (updated == null) {
            return null;
        }

        if (updated == image) {
            return this.original; // don't load a new image
        }

        return addTaskAndGet(() -> loadSkin(updated));
    }

    private static <V> V addTaskAndGet(Supplier<V> callable) {
        try {
            return mc.executeFuture(callable).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private static NativeImage getImage(Identifier res) {

        try (InputStream in = mc.getResourceManager().getResource(res).getInputStream()) {
            return NativeImage.fromInputStream(in);
        } catch (IOException e) {
            return null;
        }
    }

    @SuppressWarnings("resource")
    @Nullable
    private Identifier loadSkin(NativeImage image) {
        Identifier conv = new Identifier(original.getNamespace() + "-converted", original.getPath());
        
        if (mc.getTextureManager().registerTexture(conv, new NativeImageBackedTexture(image))) {
            return conv;
        }

        return null;
    }
}
