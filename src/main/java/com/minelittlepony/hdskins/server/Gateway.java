package com.minelittlepony.hdskins.server;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Throwables;
import com.google.common.cache.*;
import com.minelittlepony.hdskins.client.SkinUploader;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinServer.SkinServerProfile;
import com.minelittlepony.hdskins.util.net.HttpException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.*;

import net.minecraft.text.Text;

public class Gateway {
    private static final Logger LOGGER = LogManager.getLogger();

    private final SkinServer server;

    private final LoadingCache<GameProfile, CompletableFuture<Optional<SkinServer.SkinServerProfile<?>>>> profiles = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::loadUncachedProfile));

    private boolean offline;
    private boolean throttled;
    private boolean busy;

    public Gateway(SkinServer server) {
        this.server = server;
    }

    public boolean isOnline() {
        return !offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public boolean isThrottled() {
        return throttled;
    }

    public void setThrottled(boolean throttled) {
        this.throttled = throttled;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public SkinServer getServer() {
        return server;
    }

    public Stream<SkinType> getSupportedSkinTypes() {
        return SkinType.REGISTRY.stream().filter(server::supportsSkinType).distinct();
    }

    private CompletableFuture<Optional<SkinServer.SkinServerProfile<?>>> loadUncachedProfile(GameProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return server.loadProfile(profile);
            } catch (IOException | AuthenticationException e) {
                return Optional.empty();
            }
        });
    }

    public CompletableFuture<Optional<SkinServerProfile<?>>> getProfile(GameProfile profile) {
        return profiles.getUnchecked(profile);
    }

    public void invalidateProfile(GameProfile profile) {
        profiles.invalidate(profile);
    }

    public CompletableFuture<Void> uploadSkin(SkinUpload payload, Consumer<Text> errorCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                setBusy(true);
                server.uploadSkin(payload);
                invalidateProfile(payload.session().profile());
            } catch (Exception e) {
                handleException(e, errorCallback);
            } finally {
                setBusy(false);
            }
        });
    }

    public <K extends SkinServerProfile.Skin> CompletableFuture<Void> swapSkin(SkinServerProfile<K> profile, SkinType type, int index, Consumer<Text> errorCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                setBusy(true);
                profile.setActive(type, profile.getSkins(type).get(index));
                invalidateProfile(profile.getGameProfile());
            } catch (Exception e) {
                handleException(e, errorCallback);
            } finally {
                setBusy(false);
            }
        });
    }

    public CompletableFuture<TexturePayload> fetchSkins(GameProfile profile, Consumer<Text> errorCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                setBusy(true);
                return server.loadSkins(profile);
            } catch (Exception e) {
                handleException(e, errorCallback);
                throw new RuntimeException(e);
            } finally {
                setBusy(false);
            }
        });
    }

    public void handleException(Throwable throwable, Consumer<Text> errorCallback) {
        throwable = Throwables.getRootCause(throwable);

        setBusy(false);

        if (throwable instanceof HttpException) {
            HttpException ex = (HttpException)throwable;

            int code = ex.getStatusCode();

            if (code >= 500) {
                LOGGER.error(ex.getReasonPhrase(), ex);
                errorCallback.accept(Text.literal("A fatal server error has ocurred (check logs for details): \n" + ex.getReasonPhrase()));
            } else if (code >= 400 && code != 403 && code != 404) {
                LOGGER.error(ex.getReasonPhrase(), ex);
                errorCallback.accept(Text.literal(ex.getReasonPhrase()));
            } else {
                LOGGER.error(ex.getReasonPhrase(), ex);
            }
        } else {
            LOGGER.error("Unexpected error whilst contacting server at " + server.toString(), throwable);

            if (throwable instanceof AuthenticationUnavailableException) {
                setOffline(true);
            } else if (throwable instanceof InvalidCredentialsException) {
                errorCallback.accept(SkinUploader.ERR_SESSION);
            } else if (throwable instanceof AuthenticationException) {
                setThrottled(true);
            } else {
                LOGGER.error("Unhandled exception", throwable);
                errorCallback.accept(Text.literal(throwable.toString()));
            }
        }
    }
}












