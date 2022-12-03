package com.minelittlepony.hdskins.client.resources;

import java.io.*;
import java.net.URL;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.*;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public interface Texture extends AutoCloseable {
    Identifier getId();

    boolean isLoaded();

    class MemoryTexture extends NativeImageBackedTexture implements Texture {

        private final Identifier id;

        public MemoryTexture(NativeImage image, Identifier id) {
            super(image);
            this.id = id;
        }

        @Override
        public Identifier getId() {
            return id;
        }

        @Override
        public boolean isLoaded() {
            return getGlId() > -1;
        }
    }

    abstract class UriTexture extends HDPlayerSkinTexture implements Texture {

        private final String model;

        private final String fileUrl;

        private final Identifier id;

        public static Texture.UriTexture create(Identifier id, File cacheFile, String url, SkinType type, String model, Identifier fallback, @Nullable Runnable callback) {
            boolean[] uploaded = new boolean[1];
            return new UriTexture(id, cacheFile, url, type, model, fallback, () -> {
                uploaded[0] = true;
                if (callback != null) {
                    callback.run();
                }
            }) {
                @Override
                public boolean isLoaded() {
                    return uploaded[0] && getGlId() > -1;
                }

                @Override
                public void clearGlId() {
                    super.clearGlId();
                    uploaded[0] = false;
                }
            };
        }

        UriTexture(Identifier id, File cacheFile, String url, SkinType type, String model, Identifier fallback, Runnable callack) {
            super(cacheFile, url, type, fallback, callack);
            this.id = id;
            this.model = VanillaModels.of(model);
            this.fileUrl = url;
        }

        @Override
        public Identifier getId() {
            return id;
        }

        public InputStream openStream() throws IOException {
            return new URL(fileUrl).openStream();
        }

        public boolean hasModel() {
            return model != null;
        }

        public boolean usesThinArms() {
            return VanillaModels.isSlim(model);
        }

        @Override
        protected TextureData loadTextureData(ResourceManager resourceManager) {
            if (!resourceManager.getResource(location).isPresent()) {
                if (MinecraftClient.getInstance().getTextureManager().getOrDefault(location, null) instanceof NativeImageBackedTexture tex) {
                    NativeImage image = tex.getImage();
                    if (image != null) {
                        NativeImage copy = new NativeImage(image.getWidth(), image.getHeight(), true);
                        copy.copyFrom(image);
                        return new TextureData(null, copy);
                    }
                }
            }

            return super.loadTextureData(resourceManager);
        }
    }
}