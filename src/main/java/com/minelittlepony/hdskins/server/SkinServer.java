package com.minelittlepony.hdskins.server;

import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;

import java.io.IOException;
import java.util.Set;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public interface SkinServer {

    CloseableHttpClient HTTP_CLIENT = HttpClients.createSystem();

    /**
     * Returns the set of features that this skin server supports.
     */
    Set<Feature> getFeatures();

    /**
     * Determines whether this provider is the source of the provided url.
     */
    boolean ownsUrl(String url);

    /**
     * Returns whether this skin server supports a particular skin type.
     * It's recommended to implement this on an exclusion bases:
     *  return false for the things you <i>don't</i> support.
     */
    boolean supportsSkinType(SkinType skinType);

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
