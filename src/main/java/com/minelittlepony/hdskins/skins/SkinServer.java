package com.minelittlepony.hdskins.skins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.util.CallableFutures;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.util.UUIDTypeAdapter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SkinServer {

    Gson gson = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeHierarchyAdapter(SkinType.class, SkinType.adapter())
            .create();

    /**
     * Returns true for any features that this skin server supports.
     */
    boolean supportsFeature(Feature feature);

    /**
     * Synchronously loads texture information for the provided profile.
     *
     * @return The parsed server response as a textures payload.
     *
     * @throws IOException  If any authentication or network error occurs.
     */
    TexturePayload loadProfileData(GameProfile profile) throws IOException, AuthenticationException;

    /**
     * Synchronously uploads a skin to this server.
     *
     * @param upload The payload to send.
     *
     * @return A server response object.
     *
     * @throws IOException  If any authentication or network error occurs.
     * @throws AuthenticationException
     */
    void performSkinUpload(SkinUpload upload) throws IOException, AuthenticationException;

    /**
     * Asynchronously uploads a skin to the server.
     *
     * Returns an incomplete future for chaining other actions to be performed after this method completes.
     * Actions are dispatched to the default skinUploadExecutor
     *
     * @param upload The payload to send.
     */
    default CompletableFuture<Void> uploadSkin(SkinUpload upload) {
        return CallableFutures.asyncFailableFuture(() -> {
            performSkinUpload(upload);
            return null;
        }, HDSkins.skinUploadExecutor);
    }

    /**
     * Asynchronously loads texture information for the provided profile.
     *
     * Returns an incomplete future for chaining other actions to be performed after this method completes.
     * Actions are dispatched to the default skinDownloadExecutor
     */
    default CompletableFuture<TexturePayload> getPreviewTextures(GameProfile profile) {
        return CallableFutures.asyncFailableFuture(() -> loadProfileData(profile), HDSkins.skinDownloadExecutor);
    }
}
