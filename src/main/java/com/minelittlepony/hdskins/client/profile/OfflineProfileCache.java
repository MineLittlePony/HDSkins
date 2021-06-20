package com.minelittlepony.hdskins.client.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

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

import joptsimple.internal.Strings;
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
            GameProfile p = ProfileUtils.fixGameProfile(profile);
            if (p.getId() == null) {
                return null;
            }

            Path cachedLocation = getCachedProfileLocation(p);

            if (Files.exists(cachedLocation)) {
                try (JsonReader reader = new JsonReader(Files.newBufferedReader(cachedLocation))) {
                    return gson.fromJson(reader, CachedProfile.class);
                } catch (IOException e) {
                    HDSkins.LOGGER.error("Exception loading cached profile data", e);
                    try {
                        Files.delete(cachedLocation);
                    } catch (IOException ignored) {
                    }
                }
            }

            return null;
        }, Util.getMainWorkerExecutor());
    }

    private Path getCachedProfileLocation(GameProfile profile) {
        String id = profile.getId().toString();
        return repository.getHDSkinsCache().resolve("profiles").resolve(id + ".json");
    }

    public void storeCachedProfileData(GameProfile profile, Map<SkinType, MinecraftProfileTexture> textureMap) {
        try {
            profile = ProfileUtils.fixGameProfile(profile);

            Path cacheLocation = getCachedProfileLocation(profile);
            Files.createDirectories(cacheLocation.getParent());

            try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(cacheLocation))) {
                gson.toJson(new CachedProfile(textureMap), CachedProfile.class, writer);
            }
            profiles.invalidate(profile);
        } catch (IOException e) {
            HDSkins.LOGGER.trace(e);
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
        private final Map<SkinType, CachedProfileTexture> files;

        private transient Map<SkinType, MinecraftProfileTexture> compiled;

        CachedProfile(Map<SkinType, MinecraftProfileTexture> textures) {
            files = textures.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> new CachedProfileTexture(e.getValue())
            ));
        }

        synchronized Map<SkinType, MinecraftProfileTexture> getFiles() {
            if (compiled == null) {
                compiled = files.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toProfileTexture()
                ));
            }
            return compiled;
        }

        static class CachedProfileTexture {
            @Expose
            String file;

            @Nullable
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
                try {
                    Files.createDirectories(Path.of(this.file).getParent());
                } catch (IOException e) { }
            }

            public MinecraftProfileTexture toProfileTexture() {
                return new EtagProfileTexture(file, Strings.isNullOrEmpty(etag) ? "/cached" : "/cached/" + etag, metadata);
            }
        }
    }
}
