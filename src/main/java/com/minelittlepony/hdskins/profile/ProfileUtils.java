package com.minelittlepony.hdskins.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.minelittlepony.hdskins.client.HDSkins;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.client.MinecraftClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProfileUtils {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeHierarchyAdapter(SkinType.class, SkinType.adapter())
            .create();

    /**
     * Try to recreate a broken gameprofile
     * <p>
     * This happens when the server sends a random profile with skin and displayname
     */
    public static GameProfile fixGameProfile(GameProfile profile) {
        if (profile.getId() != null) {
            return profile;
        }

        try {
            return Stream.concat(
                    readCustomBlob(profile, "hd_textures", MinecraftTexturesPayload.class),
                    readCustomBlob(profile, "textures", MinecraftTexturesPayload.class)
                )
                    .filter(blob -> blob.getProfileId() != null)
                    .findFirst()
                    .map(blob -> new GameProfile(blob.getProfileId(), blob.getProfileName()))
                    .orElse(profile);
        } catch (Exception e) { // Something broke server-side probably
            HDSkins.LOGGER.warn("{} had a null UUID and was unable to recreate it from texture profile.", profile.getName(), e);
        }

        return profile;
    }

    public static Stream<Map<SkinType, MinecraftProfileTexture>> readVanillaTexturesBlob(GameProfile profile) {
        return Stream.of(MinecraftClient.getInstance().getSessionService().getTextures(profile, false))
                .filter(m -> !m.isEmpty())
                .map(m -> {
                    return m.entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    e -> SkinType.forVanilla(e.getKey()),
                                    Map.Entry::getValue
                            ));
                });
    }

    public static <T> Stream<T> readCustomBlob(GameProfile profile, String key, Class<T> type) {
        return profile.getProperties().get(key).stream().limit(1).map(textures -> {
            String json = new String(Base64.getDecoder().decode(textures.getValue()), StandardCharsets.UTF_8);

            try {
                return gson.fromJson(json, type);
            } catch (JsonParseException e) {
                HDSkins.LOGGER.error("Error reading textures blob for input: {}", json, e);
            }
            return null;
        }).filter(Objects::nonNull);
    }

    public static class TextureData {
        private Map<SkinType, MinecraftProfileTexture> textures;

        public Map<SkinType, MinecraftProfileTexture> getTextures() {
            return textures;
        }
    }
}
