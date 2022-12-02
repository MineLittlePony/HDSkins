package com.minelittlepony.hdskins.client.profile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.ProfileUtils;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.util.Util;

class ProfileCache {
    private LoadingCache<GameProfile, CompletableFuture<Map<SkinType, MinecraftProfileTexture>>> profiles = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::fetchOnlineData));

    private final HDSkins hd;

    ProfileCache(HDSkins hd) {
        this.hd = hd;
    }

    private CompletableFuture<Map<SkinType, MinecraftProfileTexture>> fetchOnlineData(GameProfile profile) {
        if (profile.getId() == null) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        return CompletableFuture.supplyAsync(() -> {
            Set<SkinType> requestedSkinTypes = SkinType.REGISTRY.stream()
                    .filter(SkinType::isKnown)
                    .collect(Collectors.toSet());

            Map<SkinType, MinecraftProfileTexture> textureMap = new HashMap<>();

            for (Gateway gateway : hd.getSkinServerList().getSkinServers()) {
                try {
                    if (!gateway.getServer().getFeatures().contains(Feature.SYNTHETIC)) {
                        gateway.getServer().loadSkins(profile).getTextures().forEach((type, texture) -> {
                            if (requestedSkinTypes.remove(type)) {
                                textureMap.putIfAbsent(type, texture);
                            }
                        });

                        if (requestedSkinTypes.isEmpty()) {
                            break;
                        }
                    }
                } catch (IOException | AuthenticationException e) {
                    HDSkins.LOGGER.trace(e);
                }
            }

            return textureMap;
        }, Util.getMainWorkerExecutor());
    }

    public CompletableFuture<Map<SkinType, MinecraftProfileTexture>> loadProfile(GameProfile profile) {
        return hd.getSkinServerList().getEmbeddedTextures(profile)
                .findFirst()
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> profiles.getUnchecked(ProfileUtils.fixGameProfile(profile)));
    }

    public void clear() {
        profiles.invalidateAll();
    }
}
