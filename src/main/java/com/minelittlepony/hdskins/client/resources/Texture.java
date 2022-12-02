package com.minelittlepony.hdskins.client.resources;

import java.io.*;
import java.net.URL;

import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
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

        public static Texture.UriTexture create(Identifier id, File cacheFile, String url, SkinType type, String model, Identifier fallback, Runnable callack) {
            boolean[] uploaded = new boolean[1];
            return new UriTexture(id, cacheFile, url, type, model, fallback, () -> {
                uploaded[0] = true;
                callack.run();
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
    }
}