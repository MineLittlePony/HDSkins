package com.minelittlepony.hdskins.skins;

import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;

import javax.annotation.Nullable;

/**
 * like {@link net.minecraft.client.util.Session}, but is available on the server.
 */
public class GameSession {

    private final String username;
    private final String uniqueId;
    @Nullable
    private final String accessToken;

    public GameSession(String username, String uniqueId, @Nullable String accessToken) {
        this.username = username;
        this.uniqueId = uniqueId;
        this.accessToken = accessToken;
    }

    public String getUsername() {
        return username;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    public GameProfile getProfile() {
        return new GameProfile(UUIDTypeAdapter.fromString(getUniqueId()), getUsername());
    }
}
