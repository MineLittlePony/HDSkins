package com.minelittlepony.hdskins.server;

import java.util.*;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

public class TexturePayload {

    private long timestamp;

    private UUID profileId;

    private String profileName;

    private boolean isPublic;

    private Map<SkinType, MinecraftProfileTexture> textures;

    TexturePayload() { }

    public TexturePayload(GameProfile profile, Map<SkinType, MinecraftProfileTexture> textures) {
        profileId = profile.getId();
        profileName = profile.getName();
        timestamp = System.currentTimeMillis();

        isPublic = true;

        this.textures = new HashMap<>(textures);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public Map<SkinType, MinecraftProfileTexture> getTextures() {
        return textures;
    }

    public Optional<MinecraftProfileTexture> getTexture(SkinType type) {
        return textures.containsKey(type) ? Optional.of(textures.get(type)) : Optional.empty();
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
                HDSkins.LOGGER.warn("Duplicate texture for skin type " + type);
            }
            return null;
        }
    }
}
