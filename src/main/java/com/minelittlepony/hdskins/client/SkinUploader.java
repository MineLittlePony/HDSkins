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
import com.minelittlepony.hdskins.client.dummy.DummyPlayer;
import com.minelittlepony.hdskins.client.dummy.EquipmentList.EquipmentSet;
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
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

public class SkinUploader implements Closeable {

    private static final Logger logger = LogManager.getLogger();

    public static final Text ERR_ALL_FINE = LiteralText.EMPTY;
    public static final Text ERR_NO_SERVER = new TranslatableText("hdskins.error.noserver");
    public static final Text ERR_OFFLINE = new TranslatableText("hdskins.error.offline");
    public static final Text ERR_SESSION = new TranslatableText("hdskins.error.session");

    public static final Text ERR_MOJANG = new TranslatableText("hdskins.error.mojang");
    public static final Text ERR_WAIT = new TranslatableText("hdskins.error.mojang.wait");

    public static final Text STATUS_FETCH = new TranslatableText("hdskins.fetch");

    private Optional<SkinServer> gateway;

    private Text status = ERR_ALL_FINE;

    private SkinType skinType = SkinType.SKIN;

    private Map<String, String> skinMetadata = new HashMap<>();

    private volatile boolean fetchingSkin = false;
    private volatile boolean throttlingNeck = false;
    private volatile boolean offline = false;

    private volatile boolean sendingSkin = false;

    private int reloadCounter = 0;
    private int retries = 1;

    private final IPreviewModel previewer;

    private final WatchedFile localSkin = new WatchedFile(this::fileChanged, this::fileRemoved);

    private final Iterator<SkinServer> skinServers;
    private final Iterator<EquipmentSet> equipmentSets;

    private EquipmentSet activeEquipmentSet;

    private final ISkinUploadHandler listener;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public SkinUploader(SkinServerList servers, IPreviewModel previewer, ISkinUploadHandler listener) {
        this.previewer = previewer;
        this.listener = listener;

        skinMetadata.put("model", VanillaModels.DEFAULT);
        skinServers = servers.getCycler();
        activeEquipmentSet = HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault();
        equipmentSets = HDSkins.getInstance().getDummyPlayerEquipmentList().getCycler();

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

    protected void setError(Text er) {
        status = er;
        sendingSkin = false;
    }

    public void setSkinType(SkinType type) {
        if (type == skinType) {
            return;
        }

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
                && !localSkin.isPending()
                && localSkin.isSet()
                && previewer.getLocal().getTextures().isUsingLocal();
    }

    public boolean canClear() {
        return !isOffline()
                && !hasStatus()
                && !downloadInProgress()
                && previewer.getRemote().getTextures().isUsingRemote();
    }

    public boolean hasStatus() {
        return status != ERR_ALL_FINE;
    }

    public Text getStatusMessage() {
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
        hasStatus();
        uploadInProgress();
        isThrottled();

        if (!hasStatus() || (!uploadInProgress() || isThrottled())) {
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
                } catch (Exception e) {
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
        } else if (throwable instanceof InvalidCredentialsException) {
            setError(ERR_SESSION);
        } else if (throwable instanceof AuthenticationException) {
            throttlingNeck = true;
        } else if (throwable instanceof HttpException) {
            HttpException ex = (HttpException)throwable;

            int code = ex.getStatusCode();

            if (code >= 500) {
                logger.error(ex.getReasonPhrase(), ex);
                setError(new TranslatableText("A fatal server error has ocurred (check logs for details): \n%s", ex.getReasonPhrase()));
            } else if (code >= 400 && code != 403 && code != 404) {
                logger.error(ex.getReasonPhrase(), ex);
                setError(new LiteralText(ex.getReasonPhrase()));
            }
        } else {
            logger.error("Unhandled exception", throwable);
            setError(new LiteralText(throwable.toString()));
        }
    }

    @Override
    public void close() throws IOException {
        previewer.getLocal().getTextures().release();
        previewer.getRemote().getTextures().release();
    }

    public void setLocalSkin(Path skinFile) {

        localSkin.set(skinFile);
    }

    public void update() {

        previewer.getLocal().updateModel();
        previewer.getRemote().updateModel();

        localSkin.update();

        if (isThrottled()) {
            reloadCounter = (reloadCounter + 1) % (200 * retries);
            if (reloadCounter == 0) {
                retries++;
                fetchRemote();
            }
        }
    }

    private void fileRemoved() {
        mc.execute(previewer.getLocal().getTextures()::release);
    }

    private void fileChanged(Path path) {
        try {
            logger.debug("Set {} {}", skinType, path);
            previewer.getLocal().getTextures().get(skinType).setLocal(path);
            listener.onSetLocalSkin(skinType);
        } catch (IOException e) {
            HDSkins.LOGGER.error("Could not load local path `" + path + "`", e);
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
