package com.minelittlepony.hdskins.profile;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minelittlepony.hdskins.client.HDSkins;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class ProfileUtils {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .create();

    /**
     * Try to recreate a broken gameprofile
     * <p>
     * This happens when the server sends a random profile with skin and displayname
     */
    public static GameProfile fixGameProfile(GameProfile profile) {
        if (profile.getId() == null) {
            try {
                MinecraftTexturesPayload texturePayload = getTexturesPayload(profile);

                if (texturePayload != null) {
                    String name = texturePayload.getProfileName(); // name is optional
                    UUID uuid = texturePayload.getProfileId();

                    if (uuid != null) {
                        return new GameProfile(uuid, name); // uuid is required
                    }
                }
            } catch (Exception e) { // Something broke server-side probably
                HDSkins.logger.warn("{} had a null UUID and was unable to recreate it from texture profile.", profile.getName(), e);
            }
        }

        return profile;
    }

    @Nullable
    public static MinecraftTexturesPayload getTexturesPayload(GameProfile profile) {
        Property textures = Iterables.getFirst(profile.getProperties().get("textures"), null);

        if (textures != null) {
            String json = new String(Base64.getDecoder().decode(textures.getValue()), StandardCharsets.UTF_8);

            return gson.fromJson(json, MinecraftTexturesPayload.class);
        }

        return null;
    }
}
