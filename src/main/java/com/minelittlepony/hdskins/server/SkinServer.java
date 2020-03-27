package com.minelittlepony.hdskins.server;

import com.minelittlepony.hdskins.server.Feature;
import com.minelittlepony.hdskins.server.SkinUpload;
import com.minelittlepony.hdskins.server.TexturePayload;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;

import java.io.IOException;
import java.util.Set;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public interface SkinServer {

    CloseableHttpClient HTTP_CLIENT = HttpClients.createSystem();

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
    TexturePayload loadProfileData(GameProfile profile) throws IOException, AuthenticationException;

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
