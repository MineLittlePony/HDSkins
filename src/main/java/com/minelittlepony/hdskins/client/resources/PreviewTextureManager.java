package com.minelittlepony.hdskins.client.resources;

import com.minelittlepony.hdskins.skins.TexturePayload;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.skins.SkinType;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Manager for fetching preview textures. This ensures that multiple calls
 * to the skin net aren't done when fetching preview textures.
 */
public class PreviewTextureManager {

    private final Map<SkinType, MinecraftProfileTexture> textures;

    public PreviewTextureManager(TexturePayload payload) {
        this.textures = payload.getTextures();
    }

    public Optional<Texture> loadServerTexture(Identifier location, SkinType type, Identifier def, SkinCallback callback) {

        if (!textures.containsKey(type)) {
            return Optional.empty();
        }

        MinecraftProfileTexture texture = textures.get(type);

        File cacheFile = tempFile(texture.getHash());

        Texture skinTexture = createTexture(cacheFile, texture.getUrl(), type, texture.getMetadata("model"), def, () -> {
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

    private Texture createTexture(File cacheFile, String url, SkinType type, String model, Identifier fallback, Runnable callack) {
        boolean[] uploaded = new boolean[1];
        return new Texture(cacheFile, url, type, model, fallback, () -> {
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

    public abstract class Texture extends HDPlayerSkinTexture {

        private final String model;

        private final String fileUrl;

        Texture(File cacheFile, String url, SkinType type, String model, Identifier fallback, Runnable callack) {
            super(cacheFile, url, type, fallback, callack);

            this.model = VanillaModels.of(model);
            this.fileUrl = url;
        }

        public abstract boolean isLoaded();

        public String getUrl() {
            return fileUrl;
        }

        public boolean hasModel() {
            return model != null;
        }

        public boolean usesThinArms() {
            return VanillaModels.isSlim(model);
        }
    }
}
