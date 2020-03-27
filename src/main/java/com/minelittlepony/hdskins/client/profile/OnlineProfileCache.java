package com.minelittlepony.hdskins.client.profile;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.ProfileUtils;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.Feature;
import com.minelittlepony.hdskins.server.SkinServer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.util.Util;

class OnlineProfileCache {
    private LoadingCache<GameProfile, CompletableFuture<Map<SkinType, MinecraftProfileTexture>>> profiles = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::fetchOnlineData));

    private final ProfileRepository repository;

    OnlineProfileCache(ProfileRepository repository) {
        this.repository = repository;
    }

    private CompletableFuture<Map<SkinType, MinecraftProfileTexture>> fetchOnlineData(GameProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            if (profile.getId() == null) {
                return Collections.emptyMap();
            }

            List<SkinType> requestedSkinTypes = SkinType.values().stream()
                    .filter(SkinType::isKnown)
                    .collect(Collectors.toList());

            Map<SkinType, MinecraftProfileTexture> textureMap = new HashMap<>();

            for (SkinServer server : repository.hd.getSkinServerList().getSkinServers()) {
                try {
                    if (!server.getFeatures().contains(Feature.SYNTHETIC)) {
                        server.loadProfileData(profile).getTextures().forEach((type, texture) -> {
                            if (requestedSkinTypes.remove(type)) {
                                textureMap.putIfAbsent(type, texture);
                            }
                        });

                        if (requestedSkinTypes.isEmpty()) {
                            break;
                        }
                    }
                } catch (IOException | AuthenticationException e) {
                    HDSkins.logger.trace(e);
                }
            }

            repository.offline.storeCachedProfileData(profile, textureMap);

            return textureMap;
        }, Util.getServerWorkerExecutor());
    }

    public CompletableFuture<Map<SkinType, MinecraftProfileTexture>> loadProfile(GameProfile profile) {
        return profiles.getUnchecked(ProfileUtils.fixGameProfile(profile));
    }

    public void clear() {
        profiles.invalidateAll();
    }
}
