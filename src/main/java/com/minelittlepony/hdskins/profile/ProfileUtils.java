package com.minelittlepony.hdskins.profile;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.minelittlepony.hdskins.HDSkinsServer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public class ProfileUtils {
    public static final String TEXTURES_KEY = "textures";
    public static final String HD_TEXTURES_KEY = "hd_textures";

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeHierarchyAdapter(SkinType.class, SkinType.adapter())
            .create();

    public static boolean hasHDTextures(GameProfile profile) {
        return profile != null && profile.properties().containsKey(HD_TEXTURES_KEY);
    }

    public static Stream<Map<SkinType, MinecraftProfileTexture>> readVanillaTexturesBlob(MinecraftSessionService service, GameProfile profile) {
        return Stream.of(unpackTextures(service.getTextures(profile))).filter(m -> !m.isEmpty());
    }

    private static Map<SkinType, MinecraftProfileTexture> unpackTextures(MinecraftProfileTextures textures) {
        Map<SkinType, MinecraftProfileTexture> result = new HashMap<>();
        putIfNotNull(result, SkinType.SKIN, textures.skin());
        putIfNotNull(result, SkinType.ELYTRA, textures.elytra());
        putIfNotNull(result, SkinType.CAPE, textures.cape());
        return result;
    }

    private static <K, V> void putIfNotNull(Map<K, V> map, K key, V value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    public static <T> Stream<T> readCustomBlob(GameProfile profile, String key, Class<T> type) {
        return profile.properties().get(key).stream().limit(1).map(textures -> {
            String json = new String(Base64.getDecoder().decode(textures.value()), StandardCharsets.UTF_8);
            try {
                return GSON.fromJson(json, type);
            } catch (JsonParseException e) {
                HDSkinsServer.LOGGER.error("Error reading textures blob for input: {}", json, e);
            }
            return null;
        }).filter(Objects::nonNull);
    }

    public static <T> GameProfile writeCustomBlob(GameProfile profile, String key, T data) {
        HashMultimap<String, Property> properties = HashMultimap.create(profile.properties());
        String json = GSON.toJson(data);
        properties.put(key, new Property(key, StandardCharsets.ISO_8859_1.decode(Base64.getEncoder().encode(StandardCharsets.UTF_8.encode(json))).toString()));
        return new GameProfile(profile.id(), profile.name(), new PropertyMap(properties));
    }

    @Deprecated
    public static void deleteProperty(GameProfile profile, String key) {
        if (profile.properties().containsKey(key)) {
            profile.properties().removeAll(key);
        }
    }

    public record TextureData (
            @SerializedName("timestamp")
            long timestamp,
            @SerializedName("textures")
            Map<SkinType, MinecraftProfileTexture> textures) {
    }
}
