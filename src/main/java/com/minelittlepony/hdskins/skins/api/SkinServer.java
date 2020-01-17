package com.minelittlepony.hdskins.skins.api;

import com.minelittlepony.hdskins.skins.Feature;
import com.minelittlepony.hdskins.skins.SkinUpload;
import com.minelittlepony.hdskins.skins.TexturePayload;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;

import java.io.IOException;
import java.util.Set;

public interface SkinServer {

    /**
     * Returns true for any features that this skin server supports.
     */
    Set<Feature> getFeatures();

    /**
     * Synchronously loads texture information for the provided profile.
     *
     * @return The parsed server response as a textures payload.
     * @throws IOException If any authentication or network error occurs.
     */
    TexturePayload loadProfileData(GameProfile profile) throws IOException;

    /**
     * Synchronously uploads a skin to this server.
     *
     * @param upload The payload to send.
     * @return A server response object.
     * @throws IOException             If any authentication or network error occurs.
     * @throws AuthenticationException
     */
    void performSkinUpload(SkinUpload upload) throws IOException, AuthenticationException;
}
