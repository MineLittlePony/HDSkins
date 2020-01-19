package com.minelittlepony.hdskins.skins;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.client.HDSkinsClient;
import com.minelittlepony.hdskins.skins.api.SkinServer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SkinServerList {

    private static final String PROPERTY_ID = "hd_textures";
    private static final String SKIN_SERVERS = "skin-servers.json";
    private static final TypeToken<Map<SkinType, MinecraftProfileTexture>> MAP_TYPE_PROFILE_TOKEN = new TypeToken<Map<SkinType, MinecraftProfileTexture>>() {};

    private static final Logger logger = LogManager.getLogger();
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(SkinServer.class, SkinServerSerializer.instance)
            .create();

    @Nullable
    private SkinServerJson skinServers;

    public void load() {
        logger.info("Loading skin servers");

        Path jsonPath = GamePaths.getConfigDirectory().resolve(SKIN_SERVERS);

        skinServers = null;

        try {
            if (Files.notExists(jsonPath)) {
                logger.info("Skin server config not found. Saving defaults.");
                try (InputStream in = getClass().getResourceAsStream("/skin-servers.json")) {
                    Files.copy(in, jsonPath);
                }
            }
            try (Reader r = Files.newBufferedReader(jsonPath)) {
                skinServers = gson.fromJson(r, SkinServerJson.class);
            }
        } catch (IOException e) {
            logger.warn("Unable to load skin servers. No servers will be used.", e);
        }
    }

    public List<SkinServer> getSkinServers() {
        return skinServers == null ? Collections.emptyList() : ImmutableList.copyOf(skinServers.servers);
    }

    public Set<String> getWhitelist() {
        return skinServers == null ? Collections.emptySet() : ImmutableSet.copyOf(skinServers.whitelist);
    }

    public Set<String> getBlacklist() {
        return skinServers == null ? Collections.emptySet() : skinServers.blacklist;
    }

    private static class SkinServerJson {
        List<SkinServer> servers = new ArrayList<>();
        Set<String> whitelist = new HashSet<>();
        Set<String> blacklist = new HashSet<>();
    }

    public Map<SkinType, MinecraftProfileTexture> loadSkin(@Nullable GameProfile profile) {
        if (profile == null || profile.getId() == null) {
            return Collections.emptyMap();
        }

        Set<SkinType> requestedSkinTypes = new HashSet<>(SkinType.values());

        Map<SkinType, MinecraftProfileTexture> textureMap = new HashMap<>();

        for (SkinServer server : getSkinServers()) {
            try {
                server.loadProfileData(profile).getTextures().forEach((type, texture) -> {
                    if (requestedSkinTypes.remove(type)) {
                        textureMap.putIfAbsent(type, texture);
                    }
                });

                if (requestedSkinTypes.isEmpty()) {
                    break;
                }
            } catch (IOException e) {
                HDSkinsClient.logger.trace(e);
            }
        }

        return textureMap;
    }

    public void fillProfileServerTextures(GameProfile profile) {
        String texturesJson;
        Map<SkinType, MinecraftProfileTexture> textures = loadSkin(profile);
        texturesJson = gson.toJson(textures);

        texturesJson = new String(Base64.getEncoder().encode(texturesJson.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        profile.getProperties().put(PROPERTY_ID, new Property(PROPERTY_ID, texturesJson));
        logger.info("Filled hd textures in profile {} ({})", profile.getName(), profile.getId());
    }

    public Optional<Map<SkinType, MinecraftProfileTexture>> getProfileServerTextures(GameProfile profile) {
        Property hdskins = Iterables.getFirst(profile.getProperties().get(PROPERTY_ID), null);
        if (hdskins != null) {
            String texturesJson = new String(Base64.getDecoder().decode(hdskins.getValue()), StandardCharsets.UTF_8);
            return Optional.of(gson.fromJson(texturesJson, MAP_TYPE_PROFILE_TOKEN.getType()));
        }
        return Optional.empty();
    }

    public CompletableFuture<Map<SkinType, MinecraftProfileTexture>> loadProfileTextures(GameProfile profile) {
        return getProfileServerTextures(profile)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> CompletableFuture.supplyAsync(() -> loadSkin(profile), Util.getServerWorkerExecutor()));
    }
}
