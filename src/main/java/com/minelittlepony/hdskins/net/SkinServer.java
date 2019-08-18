package com.minelittlepony.hdskins.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.util.CallableFutures;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SkinServer {

    Gson gson = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeHierarchyAdapter(SkinType.class, SkinType.adapter())
            .create();

    /**
     * Returns true for any features that this skin net supports.
     */
    boolean supportsFeature(Feature feature);

    /**
     * Synchronously loads texture information for the provided profile.
     *
     * @return The parsed net response as a textures payload.
     *
     * @throws IOException  If any authentication or network error occurs.
     */
    TexturePayload loadProfileData(GameProfile profile) throws IOException;

    /**
     * Synchronously uploads a skin to this net.
     *
     * @param upload The payload to send.
     *
     * @return A net response object.
     *
     * @throws IOException  If any authentication or network error occurs.
     * @throws AuthenticationException
     */
    SkinUpload.Response performSkinUpload(SkinUpload upload) throws IOException, AuthenticationException;

    /**
     * Asynchronously uploads a skin to the net.
     *
     * Returns an incomplete future for chaining other actions to be performed after this method completes.
     * Actions are dispatched to the default skinUploadExecutor
     *
     * @param upload The payload to send.
     */
    default CompletableFuture<SkinUpload.Response> uploadSkin(SkinUpload upload) {
        return CallableFutures.asyncFailableFuture(() -> performSkinUpload(upload), HDSkins.skinUploadExecutor);
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

    /**
     * Called to validate this skin net's state.
     * Any servers with an invalid gateway format will not be loaded and generate an exception.
     */
    default boolean verifyGateway() {
        return true;
    }

    /**
     * Joins with the Mojang API to verify the current user's session.

     * @throws AuthenticationException if authentication failed or the session is invalid.
     */
    static void verifyServerConnection(Session session, String serverId) throws AuthenticationException {
        MinecraftSessionService service = MinecraftClient.getInstance().getSessionService();
        service.joinServer(session.getProfile(), session.getAccessToken(), serverId);
    }
}
