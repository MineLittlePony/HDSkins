package com.minelittlepony.hdskins.profile;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.net.SkinServer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

public class OnlineProfileCache {
    private LoadingCache<GameProfile, Map<Type, MinecraftProfileTexture>> profiles = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::fetchOnlineData));


    private final ProfileRepository repository;

    OnlineProfileCache(ProfileRepository repository) {
        this.repository = repository;
    }

    private Map<Type, MinecraftProfileTexture> fetchOnlineData(GameProfile profile) {
        if (profile.getId() == null) {
            return Collections.emptyMap();
        }

        Map<Type, MinecraftProfileTexture> textureMap = Maps.newEnumMap(Type.class);

        for (SkinServer server : repository.hd.getSkinServers()) {
            try {
                server.loadProfileData(profile).getTextures().forEach(textureMap::putIfAbsent);

                if (textureMap.size() == Type.values().length) {
                    break;
                }
            } catch (IOException e) {
                HDSkins.logger.trace(e);
            }
        }

        repository.offline.storeCachedProfileData(profile, textureMap);

        return textureMap;
    }

    public CompletableFuture<Map<Type, MinecraftProfileTexture>> loadProfileAsync(GameProfile profile) {
        return CompletableFuture.supplyAsync(() -> profiles.getUnchecked(ProfileUtils.fixGameProfile(profile)), HDSkins.skinDownloadExecutor);
    }

    public void clear() {
        profiles.invalidateAll();
    }
}
