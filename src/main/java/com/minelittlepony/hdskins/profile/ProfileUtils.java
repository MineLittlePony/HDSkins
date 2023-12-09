package com.minelittlepony.hdskins.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.minelittlepony.hdskins.client.HDSkins;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.client.MinecraftClient;
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
        return profile != null && profile.getProperties().containsKey(HD_TEXTURES_KEY);
    }

    public static Stream<Map<SkinType, MinecraftProfileTexture>> readVanillaTexturesBlob(GameProfile profile) {
        return Stream.of(unpackTextures(MinecraftClient.getInstance().getSessionService().getTextures(profile))).filter(m -> !m.isEmpty());
    }

    private static Map<SkinType, MinecraftProfileTexture> unpackTextures(MinecraftProfileTextures textures) {
        Map<SkinType, MinecraftProfileTexture> result = new HashMap<>();
        if (textures.skin() != null) {
            result.put(SkinType.SKIN, textures.skin());
        }
        if (textures.elytra() != null) {
            result.put(SkinType.ELYTRA, textures.elytra());
        }
        if (textures.cape() != null) {
            result.put(SkinType.CAPE, textures.cape());
        }
        return result;
    }

    public static <T> Stream<T> readCustomBlob(GameProfile profile, String key, Class<T> type) {
        return profile.getProperties().get(key).stream().limit(1).map(textures -> {
            String json = new String(Base64.getDecoder().decode(textures.value()), StandardCharsets.UTF_8);
            try {
                return GSON.fromJson(json, type);
            } catch (JsonParseException e) {
                HDSkins.LOGGER.error("Error reading textures blob for input: {}", json, e);
            }
            return null;
        }).filter(Objects::nonNull);
    }

    public record TextureData (
            @SerializedName("textures")
            Map<SkinType, MinecraftProfileTexture> textures) {
    }
}
