package com.minelittlepony.hdskins.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
}
