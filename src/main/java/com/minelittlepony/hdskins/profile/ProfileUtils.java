package com.minelittlepony.hdskins.profile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.net.SkinServer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;

public class ProfileUtils {

    /**
     * Try to recreate a broken gameprofile
     *
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

            return SkinServer.gson.fromJson(json, MinecraftTexturesPayload.class);
        }

        return null;
    }
}
