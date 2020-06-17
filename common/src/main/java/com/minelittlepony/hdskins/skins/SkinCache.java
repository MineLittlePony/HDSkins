package com.minelittlepony.hdskins.skins;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SkinCache {

    private static final Logger logger = LogManager.getLogger();

    private final LoadingCache<GameProfile, CompletableFuture<MinecraftTexturesPayload>> CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::loadTexture));

    private final SkinServerList serverList;
    private final MinecraftSessionService sessionService;

    public SkinCache(SkinServerList servers, MinecraftSessionService sessionService) {
        this.serverList = servers;
        this.sessionService = sessionService;
    }

    private CompletableFuture<MinecraftTexturesPayload> loadTexture(GameProfile profile) {
        return CompletableFuture.supplyAsync(() -> {

            Map<Type, MinecraftProfileTexture> textures = new EnumMap<>(Type.class);
            for (SkinServer server : this.serverList) {
                if (server.getFeatures().contains(Feature.SYNTHETIC)) continue;
                if (!server.getFeatures().contains(Feature.DOWNLOAD_USER_SKIN)) continue;
                try {
                    server.loadProfileData(sessionService, profile).forEach(textures::putIfAbsent);
                } catch (IOException e) {
                    logger.error("Failed to get texture data from {}.", server, e);
                }
            }

            long timestamp = System.currentTimeMillis();
            UUID profileId = profile.getId();
            String profileName = profile.getName();
            return new TexturePayload(timestamp, profileId, profileName, true, textures);
        });
    }

    public CompletableFuture<MinecraftTexturesPayload> getPayload(GameProfile profile) {
        return CACHE.getUnchecked(profile);
    }
}
