package com.minelittlepony.hdskins.server;

import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record TexturePayload(
    long timestamp,
    UUID profileId,
    String profileName,
    boolean isPublic,
    Map<SkinType, MinecraftProfileTexture> textures) {

    public Optional<MinecraftProfileTexture> getTexture(SkinType type) {
        return textures.containsKey(type) ? Optional.of(textures.get(type)) : Optional.empty();
    }
}
