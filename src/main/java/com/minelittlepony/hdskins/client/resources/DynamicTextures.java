package com.minelittlepony.hdskins.client.resources;

import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.TexturePayload;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.file.Files;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

/**
 * Manager for fetching preview textures. This ensures that multiple calls
 * to the skin net aren't done when fetching preview textures.
 */
public class DynamicTextures {
    private final TexturePayload payload;

    private final SkinCallback loadCallback;

    public DynamicTextures(TexturePayload payload, SkinCallback loadCallback) {
        this.payload = payload;
        this.loadCallback = loadCallback;
    }

    public Optional<MinecraftProfileTexture> getTextureMetadata(SkinType type) {
        return payload.getTexture(type);
    }

    public Optional<Texture.UriTexture> loadTexture(SkinType type, Identifier def) {
        return getTextureMetadata(type).map(texture -> {
            Identifier id = new Identifier("hdskins", String.format("dynamic/%s/%s", type.getId().getPath(), texture.getHash()));
            return TextureLoader.loadTexture(id, Texture.UriTexture.create(id, createTempFile(texture.getHash()), texture.getUrl(), type, texture.getMetadata("model"), def, () -> {
                loadCallback.onSkinAvailable(type, id, texture);
            }));
        });
    }

    @Nullable
    public static File createTempFile(String filename) {
        try {
            File f = Files.createTempFile(filename, "skin-preview").toFile();
            f.delete();
            return f;
        } catch (IOException ignored) {}
        return null;
    }
}
