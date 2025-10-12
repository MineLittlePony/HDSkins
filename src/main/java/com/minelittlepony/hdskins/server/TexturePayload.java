package com.minelittlepony.hdskins.server;

import java.util.*;

import com.minelittlepony.hdskins.HDSkinsServer;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

public record TexturePayload (
    long timestamp,
    UUID profileId,
    String profileName,
    boolean isPublic,
    Textures textures
) {
    public TexturePayload(GameProfile profile, Map<SkinType, MinecraftProfileTexture> textures) {
        this(System.currentTimeMillis(), profile.id(), profile.name(), true, new Textures(textures));
    }

    public static class Textures extends HashMap<SkinType, MinecraftProfileTexture> {
        private static final long serialVersionUID = 8314133197016994678L;

        public Textures(Map<SkinType, MinecraftProfileTexture> textures) {
            putAll(textures);
        }

        public Textures() { }

        @Override
        public MinecraftProfileTexture put(SkinType type, MinecraftProfileTexture texture) {
            texture = super.put(type, texture);
            if (texture != null) {
                HDSkinsServer.LOGGER.warn("Duplicate texture for skin type " + type);
            }
            return null;
        }
    }
}
