package com.minelittlepony.hdskins.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.common.util.ProfileTextureUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.SystemUtil;

public class OfflineProfileCache {

    private static final Gson gson = new Gson();

    private final LoadingCache<GameProfile, CompletableFuture<CachedProfile>> profiles = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::fetchOfflineData));

    private final ProfileRepository repository;

    OfflineProfileCache(ProfileRepository repository) {
        this.repository = repository;
    }

    private CompletableFuture<CachedProfile> fetchOfflineData(GameProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            if (profile.getId() == null) {
                return null;
            }

            Path cachedLocation = getCachedProfileLocation(profile);

            if (Files.exists(cachedLocation)) {
                try (JsonReader reader = new JsonReader(Files.newBufferedReader(cachedLocation))) {
                    return gson.fromJson(reader, CachedProfile.class);
                } catch (IOException e) {
                    HDSkins.logger.error("Exception loading cached profile data", e);
                    try {
                        Files.delete(cachedLocation);
                    } catch (IOException ignored) {
                    }
                }
            }

            return null;
        }, HDSkins.skinDownloadExecutor);
    }

    public Path getCachedProfileLocation(GameProfile profile) {
        String id = profile.getId().toString();
        return repository.getHDSkinsCache().resolve("profiles").resolve(id + ".json");
    }

    public void storeCachedProfileData(GameProfile profile, Map<Type, MinecraftProfileTexture> textureMap) {
        try {
            Path cacheLocation = getCachedProfileLocation(profile);
            Files.createDirectories(cacheLocation.getParent());

            try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(cacheLocation))) {
                gson.toJson(new CachedProfile(textureMap), CachedProfile.class, writer);
            }
        } catch (IOException e) {
            HDSkins.logger.trace(e);
        }
    }

    public void loadProfileAsync(GameProfile profile, Consumer<Map<Type, MinecraftProfileTexture>> callback) {
        profiles.getUnchecked(ProfileUtils.fixGameProfile(profile)).thenAcceptAsync(skins -> {
            if (skins != null) {
                callback.accept(skins.getFiles());
            }
        }, MinecraftClient.getInstance());
    }

    public void clear() {
        profiles.invalidateAll();
    }

    static class CachedProfile {
        @Expose
        final Map<Type, CachedProfileTexture> files = new HashMap<>();

        CachedProfile(Map<Type, MinecraftProfileTexture> textures) {
            textures.forEach((type, texture) -> {
                files.put(type, new CachedProfileTexture(texture));
            });
        }

        Map<Type, MinecraftProfileTexture> getFiles() {
            return SystemUtil.consume(new HashMap<Type, MinecraftProfileTexture>(), m -> {
                files.forEach((type, file) -> m.put(type, file.toProfileTexture()));
            });
        }

        static class CachedProfileTexture {
            @Expose
            String file;

            @Expose
            String etag;

            @Expose
            Map<String, String> metadata;

            public CachedProfileTexture(MinecraftProfileTexture file) {
                this.file = file.getUrl();
                this.metadata = ProfileTextureUtil.getMetadataFrom(file).orElseGet(Collections::emptyMap);
                if (file instanceof EtagProfileTexture) {
                    this.etag = ((EtagProfileTexture)file).getEtag();
                }
            }

            public MinecraftProfileTexture toProfileTexture() {
                return new EtagProfileTexture(file, etag, metadata);
            }
        }
    }
}
