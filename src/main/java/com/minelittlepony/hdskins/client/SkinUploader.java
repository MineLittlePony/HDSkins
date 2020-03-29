package com.minelittlepony.hdskins.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Throwables;
import com.minelittlepony.hdskins.client.dummy.DummyPlayer;
import com.minelittlepony.hdskins.client.dummy.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.skins.Feature;
import com.minelittlepony.hdskins.skins.SkinServer;
import com.minelittlepony.hdskins.skins.SkinServerList;
import com.minelittlepony.hdskins.skins.SkinType;
import com.minelittlepony.hdskins.skins.SkinUpload;
import com.minelittlepony.hdskins.util.net.HttpException;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

public class SkinUploader implements Closeable {

    private static final Logger logger = LogManager.getLogger();

    public static final String ERR_ALL_FINE = "";
    public static final String ERR_NO_SERVER = "hdskins.error.noserver";
    public static final String ERR_OFFLINE = "hdskins.error.offline";

    public static final String ERR_MOJANG = "hdskins.error.mojang";
    public static final String ERR_WAIT = "hdskins.error.mojang.wait";

    public static final String STATUS_FETCH = "hdskins.fetch";

    private Optional<SkinServer> gateway;

    private String status = ERR_ALL_FINE;

    private SkinType skinType = SkinType.SKIN;

    private Map<String, String> skinMetadata = new HashMap<>();

    private volatile boolean fetchingSkin = false;
    private volatile boolean throttlingNeck = false;
    private volatile boolean offline = false;

    private volatile boolean sendingSkin = false;

    private int reloadCounter = 0;
    private int retries = 1;

    private final IPreviewModel previewer;

    private final Object skinLock = new Object();

    @Nullable
    private Path pendingLocalSkin;
    @Nullable
    private URI localSkin;

    private final Iterator<SkinServer> skinServers;
    private final Iterator<EquipmentSet> equipmentSets;

    private EquipmentSet activeEquipmentSet;

    private final ISkinUploadHandler listener;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public SkinUploader(SkinServerList servers, IPreviewModel previewer, ISkinUploadHandler listener) {
        this.previewer = previewer;
        this.listener = listener;

        skinMetadata.put("model", "default");
        skinServers = servers.getCycler();
        activeEquipmentSet = HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault();
        equipmentSets = HDSkins.getInstance().getDummyPlayerEquipmentList().getCycler();

        cycleGateway();
    }

    public void cycleGateway() {
        if (skinServers.hasNext()) {
            gateway = Optional.ofNullable(skinServers.next());
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

    protected void setError(String er) {
        status = er;
        sendingSkin = false;
    }

    public void setSkinType(SkinType type) {
        skinType = type;

        previewer.setSkinType(type);
        listener.onSkinTypeChanged(type);
    }

    public ItemStack cycleEquipment() {
        activeEquipmentSet = equipmentSets.next();
        return previewer.setEquipment(activeEquipmentSet);
    }

    public EquipmentSet getEquipment() {
        return activeEquipmentSet;
    }

    public boolean uploadInProgress() {
        return sendingSkin;
    }

    public boolean downloadInProgress() {
        return fetchingSkin;
    }

    public boolean isThrottled() {
        return throttlingNeck;
    }

    public boolean isOffline() {
        return offline;
    }

    public int getRetries() {
        return retries;
    }

    public boolean canUpload() {
        return !isOffline()
                && !hasStatus()
                && !uploadInProgress()
                && pendingLocalSkin == null
                && localSkin != null
                && previewer.getLocal().getTextures().isUsingLocal();
    }

    public boolean canClear() {
        return !isOffline()
                && !hasStatus()
                && !downloadInProgress()
                && previewer.getRemote().getTextures().isUsingRemote();
    }

    public boolean hasStatus() {
        return !status.isEmpty();
    }

    public String getStatusMessage() {
        return status;
    }

    public void setMetadataField(String field, String value) {
        previewer.getLocal().getTextures().release();
        skinMetadata.put(field, value);
    }

    public String getMetadataField(String field) {
        return skinMetadata.getOrDefault(field, "");
    }

    public SkinType getSkinType() {
        return skinType;
    }

    public boolean tryClearStatus() {
        if (!hasStatus() || !uploadInProgress()) {
            setError(ERR_ALL_FINE);
            return true;
        }

        return false;
    }

    public CompletableFuture<Void> uploadSkin(String statusMsg) {
        setError(statusMsg);
        sendingSkin = true;
        return CompletableFuture.runAsync(() -> {
            gateway.ifPresent(gateway -> {
                try {
                    gateway.performSkinUpload(new SkinUpload(mc.getSession(), skinType, localSkin, skinMetadata));
                    setError(ERR_ALL_FINE);
                } catch (IOException | AuthenticationException e) {
                    handleException(e);
                }
            });
        }).thenRunAsync(this::fetchRemote, MinecraftClient.getInstance());
    }

    public InputStream downloadSkin() throws IOException {
        String loc = previewer.getRemote().getTextures().get(skinType).getServerTexture().get().getUrl();
        return new URL(loc).openStream();
    }

    protected void fetchRemote() {
        throttlingNeck = false;
        offline = true;
        gateway.ifPresent(gateway -> {
            offline = false;
            fetchingSkin = true;
            previewer.getRemote().getTextures().reloadRemoteSkin(gateway, (type, location, profileTexture) -> {
                fetchingSkin = false;
                listener.onSetRemoteSkin(type, location, profileTexture);
            }).handleAsync((a, throwable) -> {
                fetchingSkin = false;

                if (throwable != null) {
                    handleException(throwable.getCause());
                } else {
                    retries = 1;
                }
                return a;
            }, MinecraftClient.getInstance());
        });
    }

    private void handleException(Throwable throwable) {
        throwable = Throwables.getRootCause(throwable);

        if (throwable instanceof AuthenticationUnavailableException) {
            offline = true;
        } else if (throwable instanceof AuthenticationException) {
            throttlingNeck = true;
        } else if (throwable instanceof HttpException) {
            HttpException ex = (HttpException)throwable;

            int code = ex.getStatusCode();

            if (code >= 500) {
                logger.error(ex.getReasonPhrase(), ex);
                setError(String.format("A fatal server error has ocurred (check logs for details): \n%s", ex.getReasonPhrase()));
            } else if (code >= 400 && code != 403 && code != 404) {
                logger.error(ex.getReasonPhrase(), ex);
                setError(ex.getReasonPhrase());
            }
        } else {
            logger.error("Unhandled exception", throwable);
            setError(throwable.toString());
        }
    }

    @Override
    public void close() throws IOException {
        previewer.getLocal().getTextures().release();
        previewer.getRemote().getTextures().release();
    }

    public void setLocalSkin(Path skinFile) {
        mc.execute(previewer.getLocal().getTextures()::release);

        synchronized (skinLock) {
            pendingLocalSkin = skinFile;
        }
    }

    public void update() {
        previewer.getLocal().updateModel();
        previewer.getRemote().updateModel();

        synchronized (skinLock) {
            if (pendingLocalSkin != null) {
                try {
                    if (Files.exists(pendingLocalSkin)) {
                        logger.debug("Set {} {}", skinType, pendingLocalSkin);
                        previewer.getLocal().getTextures().get(skinType).setLocal(pendingLocalSkin);
                        localSkin = pendingLocalSkin.toUri();
                        listener.onSetLocalSkin(skinType);
                    }
                } catch (IOException e) {
                    HDSkins.logger.error("Could not load local path `" + pendingLocalSkin + "`", e);
                } finally {
                    pendingLocalSkin = null;
                }
            }
        }

        if (isThrottled()) {
            reloadCounter = (reloadCounter + 1) % (200 * retries);
            if (reloadCounter == 0) {
                retries++;
                fetchRemote();
            }
        }
    }

    public interface IPreviewModel {
        void setSkinType(SkinType type);

        ItemStack setEquipment(EquipmentSet set);

        DummyPlayer getRemote();

        DummyPlayer getLocal();
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
