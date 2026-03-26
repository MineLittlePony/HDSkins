package com.minelittlepony.hdskins.server;

import java.io.IOException;
import java.nio.channels.UnresolvedAddressException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Throwables;
import com.google.common.cache.*;
import com.minelittlepony.hdskins.Memoize;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinServer.SkinServerProfile;
import com.minelittlepony.hdskins.server.SkinUpload.Session;
import com.minelittlepony.hdskins.util.net.HttpException;
import com.mojang.authlib.exceptions.*;

import net.minecraft.network.chat.Component;

public class Gateway {
    public static final Component ERR_SESSION = Component.translatable("hdskins.error.session");
    public static final Component ERR_DNS = Component.translatable("hdskins.error.dns");

    private static final Logger LOGGER = LogManager.getLogger();

    private final SkinServer server;

    private final LoadingCache<Session, CompletableFuture<Optional<? extends SkinServer.SkinServerProfile<?>>>> profiles;

    private boolean offline;
    private boolean throttled;
    private boolean busy;

    public Gateway(SkinServer server) {
        this.server = server;
        profiles = Memoize.createAsyncLoadingCache(15, session -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return server.loadProfile(session);
                } catch (IOException | AuthenticationException e) {
                    return Optional.empty();
                }
            });
        });
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

    public CompletableFuture<Optional<? extends SkinServerProfile<?>>> getProfile(Session session) {
        return profiles.getUnchecked(session);
    }

    public CompletableFuture<Void> uploadSkin(SkinUpload payload, Consumer<Component> errorCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                setBusy(true);
                server.uploadSkin(payload);
                profiles.invalidateAll();
            } catch (Exception e) {
                handleException(e, errorCallback);
            } finally {
                setBusy(false);
            }
        });
    }

    public <K extends SkinServerProfile.Skin> CompletableFuture<Void> swapSkin(SkinServerProfile<K> profile, SkinType type, int index, Consumer<Component> errorCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                setBusy(true);
                profile.setActive(type, profile.getSkins(type).get(index));
                profiles.invalidateAll();
            } catch (Exception e) {
                handleException(e, errorCallback);
            } finally {
                setBusy(false);
            }
        });
    }

    public CompletableFuture<TexturePayload> fetchSkins(Session session, Consumer<Component> errorCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                setBusy(true);
                return server.loadSkins(session);
            } catch (Exception e) {
                handleException(e, errorCallback);
                throw new RuntimeException(e);
            } finally {
                setBusy(false);
            }
        });
    }

    public void handleException(Throwable throwable, Consumer<Component> errorCallback) {
        throwable = Throwables.getRootCause(throwable);

        setBusy(false);

        if (throwable instanceof HttpException ex) {
            int code = ex.getStatusCode();

            if (code >= 500) {
                LOGGER.error(ex.getReasonPhrase(), ex);
                errorCallback.accept(Component.literal("A fatal server error has ocurred (check logs for details): \n" + ex.getReasonPhrase()));
            } else if (code >= 400 && code != 403 && code != 404) {
                LOGGER.error(ex.getReasonPhrase(), ex);
                errorCallback.accept(Component.literal(ex.getReasonPhrase()));
            } else {
                LOGGER.error(ex.getReasonPhrase(), ex);
            }
        } else {
            LOGGER.error("Unexpected error whilst contacting server at " + server.toString(), throwable);

            if (throwable instanceof AuthenticationUnavailableException) {
                setOffline(true);
            } else if (throwable instanceof UnresolvedAddressException) {
                errorCallback.accept(ERR_DNS);
                setOffline(true);
            } else if (throwable instanceof InvalidCredentialsException) {
                errorCallback.accept(ERR_SESSION);
            } else if (throwable instanceof AuthenticationException) {
                setThrottled(true);
            } else {
                LOGGER.error("Unhandled exception", throwable);
                errorCallback.accept(Component.literal(throwable.toString()));
            }
        }
    }
}












