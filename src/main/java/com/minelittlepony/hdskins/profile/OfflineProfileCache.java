package com.minelittlepony.hdskins.profile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import com.minelittlepony.hdskins.util.ProfileTextureUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.SystemUtil;

public class OfflineProfileCache {

    private static final Gson gson = new Gson();

    private final LoadingCache<GameProfile, Optional<CachedProfile>> profiles = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::fetchOfflineData));

    private final ProfileRepository repository;

    OfflineProfileCache(ProfileRepository repository) {
        this.repository = repository;
    }

    private Optional<CachedProfile> fetchOfflineData(GameProfile profile) {
        if (profile.getId() == null) {
            return Optional.empty();
        }

        Path cachedLocation = getCachedProfileLocation(profile);

        if (Files.exists(cachedLocation)) {
            try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(cachedLocation.toFile())))) {
                return Optional.ofNullable(gson.fromJson(reader, CachedProfile.class));
            } catch (IOException e) {
                HDSkins.logger.error("Exception loading cached profile data", e);
                try {
                    Files.delete(cachedLocation);
                } catch (IOException ignored) { }
            }
        }

        return Optional.empty();
    }

    public Path getCachedProfileLocation(GameProfile profile) {
        String id = profile.getId().toString();
        return repository.getHDSkinsCache().resolve("profiles").resolve(id + ".json");
    }

    public void storeCachedProfileData(GameProfile profile, Map<Type, MinecraftProfileTexture> textureMap) {
        try {
            Path cacheLocation = getCachedProfileLocation(profile);
            if (Files.exists(cacheLocation)) {
                Files.delete(cacheLocation);
            }
            Files.createDirectories(cacheLocation.getParent());
            Files.createFile(cacheLocation);

            try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(cacheLocation.toFile())))) {
                gson.toJson(new CachedProfile(textureMap), CachedProfile.class, writer);
            }
        } catch (IOException e) {
            HDSkins.logger.trace(e);
        }
    }

    public void loadProfileAsync(GameProfile profile, Consumer<Map<Type, MinecraftProfileTexture>> callback) {
        CompletableFuture.runAsync(() -> {
            profiles.getUnchecked(ProfileUtils.fixGameProfile(profile)).map(CachedProfile::getFiles).ifPresent(callback);
        }, MinecraftClient.getInstance()::execute);
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
                this.metadata = ProfileTextureUtil.getMetadataFrom(file).orElse(Collections.emptyMap());
                if (file instanceof EtagProfileTexture) {
                    etag = ((EtagProfileTexture)file).getEtag();
                }
            }

            public MinecraftProfileTexture toProfileTexture() {
                return new EtagProfileTexture(file, etag, metadata);
            }
        }
    }
}
