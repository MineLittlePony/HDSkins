package com.minelittlepony.hdskins.skins;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;

import java.util.Map;
import java.util.UUID;

public class TexturePayload extends MinecraftTexturesPayload {
    private final long timestamp;
    private final UUID profileId;
    private final String profileName;
    private final boolean isPublic;
    private final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures;

    public TexturePayload(long timestamp, UUID profileId, String profileName, boolean isPublic, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures) {
        this.timestamp = timestamp;
        this.profileId = profileId;
        this.profileName = profileName;
        this.isPublic = isPublic;
        this.textures = textures;
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

    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures() {
        return textures;
    }
}
