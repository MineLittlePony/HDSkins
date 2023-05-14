package com.minelittlepony.hdskins.server;

import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record TexturePayload(
        long timestamp,
        UUID profileId,
        String profileName,
        boolean isPublic,
        Map<SkinType, MinecraftProfileTexture> textures) {

    public TexturePayload(GameProfile profile, Map<SkinType, MinecraftProfileTexture> textures) {
        this(System.currentTimeMillis(), profile.getId(), profile.getName(), true, new HashMap<>(textures));
    }

    /**
     * @deprecated Use {@link #timestamp()} instead.
     */
    @Deprecated
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @deprecated Use {@link #profileId()} instead.
     */
    @Deprecated
    public UUID getProfileId() {
        return profileId;
    }

    /**
     * @deprecated Use {@link #profileName()} instead.
     */
    @Deprecated
    public String getProfileName() {
        return profileName;
    }

    /**
     * @deprecated Use {@link #textures()} instead.
     */
    @Deprecated
    public Map<SkinType, MinecraftProfileTexture> getTextures() {
        return textures;
    }

    public Optional<MinecraftProfileTexture> getTexture(SkinType type) {
        return textures.containsKey(type) ? Optional.of(textures.get(type)) : Optional.empty();
    }
}
