package com.minelittlepony.hdskins;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

import org.apache.http.client.methods.RequestBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Throwables;
import com.minelittlepony.hdskins.dummy.DummyPlayer;
import com.minelittlepony.hdskins.dummy.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.net.Feature;
import com.minelittlepony.hdskins.net.SkinServer;
import com.minelittlepony.hdskins.net.SkinServerList;
import com.minelittlepony.hdskins.net.SkinUpload;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.util.CallableFutures;
import com.minelittlepony.hdskins.util.net.HttpException;
import com.minelittlepony.hdskins.util.net.MoreHttpResponses;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class SkinUploader implements Closeable {

    private static final Logger logger = LogManager.getLogger();

    public static final String ERR_ALL_FINE = "";
    public static final String ERR_NO_SERVER = "hdskins.error.noserver";
    public static final String ERR_OFFLINE = "hdskins.error.offline";

    public static final String ERR_MOJANG = "hdskins.error.mojang";
    public static final String ERR_WAIT = "hdskins.error.mojang.wait";

    public static final String STATUS_FETCH = "hdskins.fetch";

    @Nullable
    private SkinServer gateway;

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

    private Path pendingLocalSkin;
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
            gateway = skinServers.next();
            fetchRemote();
        } else {
            setError(ERR_NO_SERVER);
        }
    }

    public String getGatewayText() {
        return gateway == null ? "" : gateway.toString();
    }

    public SkinServer getGateway() {
        return gateway;
    }

    public boolean supportsFeature(Feature feature) {
        return gateway != null && gateway.supportsFeature(feature);
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
            status = ERR_ALL_FINE;
            return true;
        }

        return false;
    }

    public CompletableFuture<Void> uploadSkin(String statusMsg) {
        sendingSkin = true;
        status = statusMsg;

        return gateway.uploadSkin(new SkinUpload(mc.getSession(), skinType, localSkin == null ? null : localSkin, skinMetadata)).handleAsync((response, throwable) -> {
            if (throwable == null) {
                logger.info("Upload completed with: %s", response);
                setError(ERR_ALL_FINE);
            } else {
                setError(Throwables.getRootCause(throwable).toString());
            }

            fetchRemote();
            return null;
        }, MinecraftClient.getInstance());
    }

    public CompletableFuture<MoreHttpResponses> downloadSkin() {
        String loc = previewer.getRemote().getTextures().get(skinType).getRemote().getUrl();

        return CallableFutures.asyncFailableFuture(() -> {
            return MoreHttpResponses.execute(HDSkins.httpClient, RequestBuilder.get().setUri(loc).build());
        }, HDSkins.skinDownloadExecutor);
    }

    protected void fetchRemote() {
        fetchingSkin = true;
        throttlingNeck = false;
        offline = false;

        previewer.getRemote().getTextures().reloadRemoteSkin(this, (type, location, profileTexture) -> {
            fetchingSkin = false;
            listener.onSetRemoteSkin(type, location, profileTexture);
        }).handleAsync((a, throwable) -> {
            fetchingSkin = false;

            if (throwable != null) {
                throwable = throwable.getCause();

                if (throwable instanceof AuthenticationUnavailableException) {
                    offline = true;
                } else if (throwable instanceof AuthenticationException) {
                    throttlingNeck = true;
                } else if (throwable instanceof HttpException) {
                    HttpException ex = (HttpException)throwable;

                    logger.error(ex.getReasonPhrase(), ex);

                    int code = ex.getStatusCode();

                    if (code >= 500) {
                        setError(String.format("A fatal server error has ocurred (check logs for details): \n%s", ex.getReasonPhrase()));
                    } else if (code >= 400 && code != 403 && code != 404) {
                        setError(ex.getReasonPhrase());
                    }
                } else {
                    logger.error("Unhandled exception", throwable);
                    setError(throwable.toString());
                }
            }
            return a;
        }, MinecraftClient.getInstance());
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
                logger.debug("Set %s %s", skinType, pendingLocalSkin);
                previewer.getLocal().getTextures().setLocal(pendingLocalSkin, skinType);
                localSkin = pendingLocalSkin.toUri();
                pendingLocalSkin = null;
                listener.onSetLocalSkin(skinType);
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
