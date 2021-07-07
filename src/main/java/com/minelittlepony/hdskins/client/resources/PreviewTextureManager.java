package com.minelittlepony.hdskins.client.resources;

import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.TexturePayload;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

/**
 * Manager for fetching preview textures. This ensures that multiple calls
 * to the skin net aren't done when fetching preview textures.
 */
public class PreviewTextureManager {

    private final Map<SkinType, MinecraftProfileTexture> textures;

    public PreviewTextureManager(TexturePayload payload) {
        this.textures = payload.getTextures();
    }

    public Optional<UriTexture> loadServerTexture(SkinType type, Identifier def, SkinCallback callback) {

        if (!textures.containsKey(type)) {
            return Optional.empty();
        }

        MinecraftProfileTexture texture = textures.get(type);

        File cacheFile = tempFile(texture.getHash());

        Identifier location = new Identifier("hdskins", String.format("dynamic/%s/%s", type.getId().getPath(), texture.getHash()));

        UriTexture skinTexture = createTexture(location, cacheFile, texture.getUrl(), type, texture.getMetadata("model"), def, () -> {
            callback.onSkinAvailable(type, location, new MinecraftProfileTexture(texture.getUrl(), new HashMap<>()));
        });

        TextureLoader.loadTexture(location, skinTexture);

        return Optional.of(skinTexture);
    }

    @Nullable
    private File tempFile(String filename) {
        try {
            File f = Files.createTempFile(filename, "skin-preview").toFile();
            f.delete();
            return f;
        } catch (IOException ignored) {}
        return null;
    }

    private UriTexture createTexture(Identifier id, File cacheFile, String url, SkinType type, String model, Identifier fallback, Runnable callack) {
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

    public interface Texture {
        Identifier getId();

        boolean isLoaded();
    }

    public static class FileTexture extends NativeImageBackedTexture implements Texture {

        private final Identifier id;

        public FileTexture(NativeImage image, Identifier id) {
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

    public static abstract class UriTexture extends HDPlayerSkinTexture implements Texture {

        private final String model;

        private final String fileUrl;

        private final Identifier id;

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
