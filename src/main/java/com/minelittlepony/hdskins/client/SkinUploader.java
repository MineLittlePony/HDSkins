package com.minelittlepony.hdskins.client;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Throwables;
import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.hdskins.client.dummy.DummyPlayer;
import com.minelittlepony.hdskins.client.dummy.PlayerPreview;
import com.minelittlepony.hdskins.client.dummy.TextureProxy;
import com.minelittlepony.hdskins.client.resources.PreviewTextureManager;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.Feature;
import com.minelittlepony.hdskins.server.SkinServer;
import com.minelittlepony.hdskins.server.SkinServerList;
import com.minelittlepony.hdskins.server.SkinUpload;
import com.minelittlepony.hdskins.util.net.HttpException;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.Collections;
import java.util.Set;

public class SkinUploader implements Closeable {

    private static final Logger logger = LogManager.getLogger();

    public static final Text ERR_ALL_FINE = ScreenTexts.EMPTY;
    public static final Text ERR_NO_SERVER = Text.translatable("hdskins.error.noserver");
    public static final Text ERR_OFFLINE = Text.translatable("hdskins.error.offline");
    public static final Text ERR_SESSION = Text.translatable("hdskins.error.session");

    public static final Text ERR_MOJANG = Text.translatable("hdskins.error.mojang");
    public static final String ERR_MOJANG_WAIT = "hdskins.error.mojang.wait";

    public static final Text STATUS_FETCH = Text.translatable("hdskins.fetch");

    private Optional<SkinServer> gateway;

    private Text errorMessage = ERR_ALL_FINE;

    private SkinType skinType = SkinType.SKIN;

    private Map<String, String> skinMetadata = new HashMap<>();

    private volatile boolean fetchingSkin = false;
    private volatile boolean throttlingNeck = false;
    private volatile boolean offline = false;
    private volatile boolean pending = false;

    private volatile boolean sendingSkin = false;

    private int reloadCounter = 0;
    private int retries = 1;

    private final PlayerPreview previewer;

    private final WatchedFile localSkin = new WatchedFile(this::fileChanged, this::fileRemoved);

    private final Iterator<SkinServer> skinServers;

    private final ISkinUploadHandler listener;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public SkinUploader(SkinServerList servers, PlayerPreview previewer, ISkinUploadHandler listener) {
        this.previewer = previewer;
        this.listener = listener;

        skinMetadata.put("model", VanillaModels.DEFAULT);
        skinServers = servers.getCycler();

        cycleGateway();
    }

    public void cycleGateway() {
        if (skinServers.hasNext()) {
            gateway = Optional.ofNullable(skinServers.next());
            setSkinType(gateway.flatMap(g -> g.supportsSkinType(skinType) ? Optional.of(skinType) : getSupportedSkinTypes().stream().findFirst()).orElse(SkinType.UNKNOWN));
            fetchRemote();
        } else {
            setError(ERR_NO_SERVER);
        }
    }

    public String getGatewayText() {
        return gateway.map(SkinServer::toString).orElse("");
    }

    public Set<Feature> getFeatures() {
        return gateway.map(SkinServer::getFeatures).orElseGet(Collections::emptySet);
    }

    public List<SkinType> getSupportedSkinTypes() {
        return gateway.map(g -> {
            return SkinType.REGISTRY.stream().filter(g::supportsSkinType).collect(Collectors.toList());
        }).orElse(Collections.emptyList());
    }

    public void setSkinType(SkinType type) {
        if (type == skinType) {
            return;
        }

        skinType = type;

        previewer.setSkinType(type);
        listener.onSkinTypeChanged(type);
    }

    public boolean uploadInProgress() {
        return sendingSkin;
    }

    public boolean isThrottled() {
        return throttlingNeck;
    }

    public int getRetries() {
        return retries;
    }

    public boolean canUpload() {
        return !offline
                && !hasError()
                && !uploadInProgress()
                && !localSkin.isPending()
                && localSkin.isSet()
                && previewer.getLocal().map(DummyPlayer::getTextures).filter(TextureProxy::isUsingLocal).isPresent();
    }

    public boolean canClear() {
        return !offline
                && !hasError()
                && !fetchingSkin
                && previewer.getRemote().map(DummyPlayer::getTextures).filter(TextureProxy::isUsingRemote).isPresent();
    }

    public boolean hasError() {
        return errorMessage != ERR_ALL_FINE;
    }

    public Text getError() {
        return errorMessage;
    }

    protected void setError(Text er) {
        errorMessage = er;
        sendingSkin = false;
    }

    public boolean hasStatus() {
        return fetchingSkin || isThrottled() || offline || !previewer.getRemote().isPresent();
    }

    public Text getStatus() {
        if (isThrottled()) {
            return ERR_MOJANG;
        }
        if (offline || errorMessage == ERR_SESSION) {
            return  ERR_OFFLINE;
        }
        return STATUS_FETCH;
    }

    public void setMetadataField(String field, String value) {
        previewer.getLocal().map(DummyPlayer::getTextures).ifPresent(TextureProxy::dispose);
        skinMetadata.put(field, value);
    }

    public String getMetadataField(String field) {
        return skinMetadata.getOrDefault(field, "");
    }

    public SkinType getSkinType() {
        return skinType;
    }

    public boolean tryClearStatus() {
        hasError();
        uploadInProgress();
        isThrottled();

        if (!hasError() || (!uploadInProgress() || isThrottled())) {
            setError(ERR_ALL_FINE);
            return true;
        }

        return false;
    }

    public CompletableFuture<Void> uploadSkin(Text statusMsg) {
        setError(statusMsg);
        sendingSkin = true;
        return CompletableFuture.runAsync(() -> {
            gateway.ifPresent(gateway -> {
                try {
                    gateway.performSkinUpload(new SkinUpload(mc.getSession(), skinType, localSkin.toUri(), skinMetadata));
                    setError(ERR_ALL_FINE);
                } catch (IOException | AuthenticationException e) {
                    handleException(e);
                }
            });
        }).thenRunAsync(this::fetchRemote, MinecraftClient.getInstance());
    }

    public Optional<PreviewTextureManager.UriTexture> getServerTexture() {
        return previewer.getRemote().map(DummyPlayer::getTextures).flatMap(t -> t.get(skinType).getServerTexture());
    }

    protected void fetchRemote() {
        boolean wasPending = pending;
        pending = false;
        throttlingNeck = false;
        offline = true;
        gateway.ifPresent(gateway -> {
            previewer.getRemote().map(DummyPlayer::getTextures).ifPresentOrElse(t -> {
                offline = false;
                fetchingSkin = true;
                t.reloadRemoteSkin(gateway, (type, location, profileTexture) -> {
                    if (type == skinType) {
                        fetchingSkin = false;
                        if (wasPending) {
                            GameGui.playSound(SoundEvents.ENTITY_VILLAGER_YES);
                        }
                    }
                    listener.onSetRemoteSkin(type, location, profileTexture);
                }).handleAsync((a, throwable) -> {

                    if (throwable != null) {
                        handleException(throwable.getCause());
                    } else {
                        retries = 1;
                    }
                    return a;
                }, MinecraftClient.getInstance());
            }, () -> {
                offline = false;
                pending = true;
            });
        });
    }

    private void handleException(Throwable throwable) {
        throwable = Throwables.getRootCause(throwable);

        fetchingSkin = false;

        if (throwable instanceof AuthenticationUnavailableException) {
            offline = true;
        } else if (throwable instanceof InvalidCredentialsException) {
            setError(ERR_SESSION);
        } else if (throwable instanceof AuthenticationException) {
            throttlingNeck = true;
        } else if (throwable instanceof HttpException) {
            HttpException ex = (HttpException)throwable;

            int code = ex.getStatusCode();

            if (code >= 500) {
                logger.error(ex.getReasonPhrase(), ex);
                setError(Text.literal("A fatal server error has ocurred (check logs for details): \n" + ex.getReasonPhrase()));
            } else if (code >= 400 && code != 403 && code != 404) {
                logger.error(ex.getReasonPhrase(), ex);
                setError(Text.literal(ex.getReasonPhrase()));
            }
        } else {
            logger.error("Unhandled exception", throwable);
            setError(Text.literal(throwable.toString()));
        }
    }

    @Override
    public void close() throws IOException {
        previewer.apply(p -> p.getTextures().dispose());
    }

    public void setLocalSkin(Path skinFile) {
        localSkin.set(skinFile);
    }

    public void update() {
        if (!previewer.getRemote().isPresent()) {
            return;
        }

        previewer.apply(DummyPlayer::updateModel);
        localSkin.update();

        if (isThrottled()) {
            reloadCounter = (reloadCounter + 1) % (200 * retries);
            if (reloadCounter == 0) {
                retries++;
                fetchRemote();
            }
        } else if (pending) {
            fetchRemote();
        }
    }

    private void fileRemoved() {
        previewer.getLocal().map(DummyPlayer::getTextures).ifPresent(t -> mc.execute(t::dispose));
    }

    private void fileChanged(Path path) {
        previewer.getLocal().map(DummyPlayer::getTextures).ifPresent(t -> {
            try {
                logger.debug("Set {} {}", skinType, path);
                t.get(skinType).setLocal(path);
                listener.onSetLocalSkin(skinType);
            } catch (IOException e) {
                HDSkins.LOGGER.error("Could not load local path `" + path + "`", e);
            }
        });
    }

    public interface ISkinUploadHandler {
        default void onSetRemoteSkin(SkinType type, Identifier location, MinecraftProfileTexture profileTexture) {
        }

        default void onSetLocalSkin(SkinType type) {
        }

        default void onSkinTypeChanged(SkinType newType) {

        }
    }
}
