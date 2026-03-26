package com.minelittlepony.hdskins.client.resources;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.TexturePayload;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.resources.Identifier;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
        return Optional.ofNullable(payload.textures().getOrDefault(type, null));
    }

    public Optional<Result> loadTexture(SkinType type, Identifier def) {
        return getTextureMetadata(type).map(texture -> {
            Identifier id = HDSkins.id(String.format("dynamic/%s/%s", type.getId().getPath(), texture.getHash()));
            String uri = texture.getUrl();
            return new Result(uri, HDPlayerSkinTextureDownloader.downloadAndRegisterTexture(id, createTempFile(texture.getHash()), uri, type).handle((u, _) -> {
                if (u != null) {
                    loadCallback.onSkinAvailable(type, u, texture);
                }
                return u == null ? def : u;
            }));
        });
    }

    @Nullable
    public static Path createTempFile(String filename) {
        try {
            Path f = Files.createTempFile(filename, "skin-preview");
            Files.deleteIfExists(f);
            return f;
        } catch (IOException ignored) {}
        return null;
    }

    public record Result(
            String uri,
            CompletableFuture<Identifier> id
    ) {
        @SuppressWarnings("deprecation")
        public InputStream openStream() throws IOException {
            return new URL(uri).openStream();
        }
    }
}
