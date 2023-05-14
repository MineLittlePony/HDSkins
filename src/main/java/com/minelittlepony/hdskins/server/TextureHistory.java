package com.minelittlepony.hdskins.server;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public record TextureHistory(
        UUID profileId,
        String profileName,
        Map<String, Collection<TextureHistoryEntry>> textures) {

    public GameProfile gameProfile() {
        return new GameProfile(profileId, profileName);
    }

    public record TextureHistoryEntry(
            String url,
            Map<String, String> metadata,
            long startTime,
            @Nullable
            Long endTime) {
    }
}
