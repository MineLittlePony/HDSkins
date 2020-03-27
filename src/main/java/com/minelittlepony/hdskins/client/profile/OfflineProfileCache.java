package com.minelittlepony.hdskins.client.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.EtagProfileTexture;
import com.minelittlepony.hdskins.profile.ProfileUtils;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.common.util.ProfileTextureUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.util.Util;

class OfflineProfileCache {

    private static final Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(SkinType.class, SkinType.adapter())
            .create();

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
        }, Util.getServerWorkerExecutor());
    }

    private Path getCachedProfileLocation(GameProfile profile) {
        String id = profile.getId().toString();
        return repository.getHDSkinsCache().resolve("profiles").resolve(id + ".json");
    }

    public void storeCachedProfileData(GameProfile profile, Map<SkinType, MinecraftProfileTexture> textureMap) {
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

    public CompletableFuture<Map<SkinType, MinecraftProfileTexture>> loadProfile(GameProfile profile) {
        return profiles
                .getUnchecked(ProfileUtils.fixGameProfile(profile))
                .thenApply(f -> f == null ? Maps.newHashMap() : f.getFiles());
    }

    public void clear() {
        profiles.invalidateAll();
    }

    static class CachedProfile {
        @Expose
        final Map<SkinType, CachedProfileTexture> files = new HashMap<>();

        CachedProfile(Map<SkinType, MinecraftProfileTexture> textures) {
            textures.forEach((type, texture) -> {
                files.put(type, new CachedProfileTexture(texture));
            });
        }

        Map<SkinType, MinecraftProfileTexture> getFiles() {
            return Util.make(new HashMap<>(), m -> {
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
