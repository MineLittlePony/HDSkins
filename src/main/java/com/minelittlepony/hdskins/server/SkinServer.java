package com.minelittlepony.hdskins.server;

import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;

import java.io.IOException;
import java.util.*;

public interface SkinServer {
    /**
     * Returns the set of features that this skin server supports.
     */
    Set<Feature> getFeatures();

    /**
     * Determines whether this server is the source of the provided url.
     */
    boolean ownsUrl(String url);

    /**
     * Returns whether this skin server supports a particular skin type.
     * It's recommended to implement this on an exclusion bases:
     *  return false for the things you <i>don't</i> support.
     */
    boolean supportsSkinType(SkinType skinType);

    /**
     * Loads texture information for the provided profile.
     *
     * @return The parsed server response as a textures payload.
     * @throws IOException If any authentication or network error occurs.
     */
    TexturePayload loadSkins(GameProfile profile) throws IOException, AuthenticationException;

    /**
     * Load texture history for the provided profile.
     *
     * @param profile The profile whose history to load
     * @return The texture history
     *
     * @throws IOException
     */
    TextureHistory loadSkinHistory(GameProfile profile) throws IOException;

    /**
     * Uploads a player's skin to this server.
     *
     * @param upload The payload to send.
     * @return A server response object.
     *
     * @throws IOException
     * @throws AuthenticationException
     */
    void uploadSkin(SkinUpload upload) throws IOException, AuthenticationException;

    /***
     * Loads a player's detailed profile from this server.
     * @param profile The game profile of the player being queried
     * @return The pre-populated profile of the given player.
     * @throws IOException
     * @throws AuthenticationException
     */
    default Optional<SkinServerProfile<?>> loadProfile(GameProfile profile) throws IOException, AuthenticationException {
        return Optional.empty();
    }

    interface SkinServerProfile<T extends SkinServerProfile.Skin> {
        GameProfile getGameProfile();

        List<T> getSkins(SkinType type);

        void setActive(SkinType type, T texture);

        interface Skin {
            String getModel();

            boolean isActive();

            String getUri();
        }
    }

}
