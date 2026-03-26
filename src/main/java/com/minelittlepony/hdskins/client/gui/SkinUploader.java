package com.minelittlepony.hdskins.client.gui;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.gui.player.skins.ServerPlayerSkins;
import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.*;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Uploader contains form data and server communication logic.
 */
public class SkinUploader implements Closeable, CarouselStatusLabel {
    public static final Component STATUS_OK = CommonComponents.EMPTY;
    public static final Component STATUS_NO_SERVER = Component.translatable("hdskins.error.noserver");
    public static final Component STATUS_OFFLINE = Component.translatable("hdskins.error.offline");
    public static final Component STATUS_SESSION = Component.translatable("hdskins.error.session.short");

    public static final Component STATUS_MOJANG = Component.translatable("hdskins.error.mojang");
    public static final Component STATUS_BUSY = Component.translatable("hdskins.status.busy");
    public static final String ERR_MOJANG_WAIT = "hdskins.error.mojang.wait";

    public static final Component STATUS_FETCH = Component.translatable("hdskins.fetch");

    private Component bannerMessage = STATUS_OK;

    private Map<String, String> skinMetadata = new HashMap<>();

    private volatile boolean pendingRefresh = false;

    private int reloadCounter = 0;
    private int retries = 1;

    private final DualCarouselWidget<?> previewer;

    private final Iterator<Gateway> gateways;
    private Optional<Gateway> gateway;

    private SkinCallback loadListener = SkinCallback.NOOP;
    private Consumer<SkinType> skinTypeChangedListener = _ -> {};

    private final SkinUpload.Session session;

    public SkinUploader(Iterator<Gateway> gateways, DualCarouselWidget<?> previewer, SkinUpload.Session session) {
        this.previewer = previewer;
        this.gateways = gateways;
        this.session = session;
        skinMetadata.put("model", VanillaModels.DEFAULT);
        cycleGateway();
    }

    public void addSkinLoadedEventListener(SkinCallback listener) {
        this.loadListener = this.loadListener.andThen(listener);
    }

    public void addSkinTypeChangedEventListener(Consumer<SkinType> listener) {
        this.skinTypeChangedListener = this.skinTypeChangedListener.andThen(listener);
    }

    public Map<String, String> getMetadata() {
        return skinMetadata;
    }

    public void cycleGateway() {
        if (gateways.hasNext()) {
            gateway = Optional.ofNullable(gateways.next());
            setSkinType(gateway.flatMap(g -> g.getServer().supportsSkinType(previewer.getActiveSkinType())
                    ? Optional.of(previewer.getActiveSkinType())
                    : getSupportedSkinTypes().findFirst()
            ).orElse(SkinType.UNKNOWN));
            scheduleReload();
        } else {
            setBannerMessage(STATUS_NO_SERVER);
        }
    }

    public SkinUpload.Session getSession() {
        return session;
    }

    public Optional<Gateway> getGateway() {
        return gateway;
    }

    public String getGatewayText() {
        return gateway.map(Gateway::getServer).map(SkinServer::toString).orElse("");
    }

    public Set<Feature> getFeatures() {
        return gateway.map(Gateway::getServer).map(server -> server.getFeatures(previewer.getActiveSkinType())).orElse(Set.of());
    }

    public Stream<SkinType> getSupportedSkinTypes() {
        return gateway.stream().flatMap(Gateway::getSupportedSkinTypes);
    }

    public void setSkinType(SkinType type) {
        if (type == previewer.getActiveSkinType()) {
            return;
        }
        previewer.setSkinType(type);
        skinTypeChangedListener.accept(type);
    }

    public boolean isThrottled() {
        return gateway.filter(Gateway::isThrottled).isPresent();
    }

    public int getRetries() {
        return retries;
    }

    private boolean isOnline() {
        return gateway.filter(Gateway::isOnline).isPresent()
                && getBannerMessage() != Gateway.ERR_SESSION;
    }

    public boolean isBusy() {
        return gateway.filter(Gateway::isBusy).isPresent();
    }

    private boolean isSkinOperationsBlocked() {
        return hasStatus() || hasBannerMessage() || isBusy();
    }

    public boolean canUpload(SkinType type) {
        return getFeatures().contains(Feature.UPLOAD_USER_SKIN) && !isSkinOperationsBlocked() && previewer.getLocal().getSkins().get(type).isReady();
    }

    public boolean canClear(SkinType type) {
        return getFeatures().contains(Feature.DELETE_USER_SKIN) && !isSkinOperationsBlocked() && hasUploaded(type);
    }

    public boolean hasUploaded(SkinType type) {
        return previewer.getRemote().getSkins().get(type).isReady();
    }

    public boolean canClearAny() {
        return getFeatures().contains(Feature.DELETE_USER_SKIN) && !isSkinOperationsBlocked() && previewer.getRemote().getSkins().hasAny();
    }

    public boolean hasBannerMessage() {
        return bannerMessage != STATUS_OK;
    }

    public Component getBannerMessage() {
        return bannerMessage;
    }

    public void setBannerMessage(Component er) {
        bannerMessage = er;
    }

    @Override
    public boolean hasStatus() {
        return getStatus() != STATUS_OK;
    }

    private Component getStatus() {
        if (isBusy()) {
            return STATUS_BUSY;
        }

        if (gateway.isEmpty()) {
            return STATUS_OFFLINE;
        }

        if (isThrottled()) {
            return STATUS_MOJANG;
        }

        if (!isOnline()) {
            return STATUS_OFFLINE;
        }

        if (session.hasFailedValidation()) {
            return STATUS_SESSION;
        }

        return STATUS_OK;
    }

    @Override
    public List<Component> getStatusLines() {
        Component status = getStatus();
        if (status == STATUS_MOJANG) {
            return List.of(status, Component.translatable(ERR_MOJANG_WAIT, getRetries()));
        }
        return List.of(status);
    }

    @Override
    public int getLabelColor(Component status) {
        return isThrottled() || status == STATUS_SESSION || status == STATUS_OFFLINE ? RED : WHITE;
    }

    public void setMetadataField(String field, String value) {
        skinMetadata.put(field, value);
    }

    public String getMetadataField(String field) {
        return skinMetadata.getOrDefault(field, "");
    }

    public boolean tryClearStatus() {
        if (!hasBannerMessage() || !(isBusy() || isThrottled())) {
            setBannerMessage(STATUS_OK);
            return true;
        }

        return false;
    }

    public CompletableFuture<Void> uploadSkin(Component statusMsg, SkinUpload payload) {
        setBannerMessage(statusMsg);
        return gateway
                .map(g -> g.uploadSkin(payload, this::setBannerMessage))
                .map(future -> future.thenRunAsync(this::scheduleReload, Minecraft.getInstance()))
                .orElseGet(() -> CompletableFuture.failedFuture(new IOException("No gateway"))).whenComplete((_, t) -> {
                    if (t != null) {
                        HDSkins.LOGGER.fatal("Exception caught whilst uploading skin", t);
                    }
                });
    }

    public void scheduleReload() {
        pendingRefresh = true;
    }

    protected void fetchRemote() {
        pendingRefresh = false;
        gateway.ifPresent(gateway -> {
            gateway
                .fetchSkins(session, this::setBannerMessage)
                .thenAcceptAsync(textures -> {
                    ServerPlayerSkins skins = previewer.getRemote().getSkins();
                    skins.loadTextures(textures, loadListener);
                    gateway.getProfile(session).thenAccept(skins::loadProfile);
                }, Minecraft.getInstance())
                .handleAsync((a, throwable) -> {
                    if (throwable == null) {
                        retries = 1;
                    }
                    return a;
                }, Minecraft.getInstance());
        });
    }

    @Override
    public void close() throws IOException {
        previewer.close();
    }

    public void update() {
        if (isThrottled()) {
            reloadCounter = (reloadCounter + 1) % (200 * retries);
            if (reloadCounter == 0) {
                retries++;
                fetchRemote();
            }
        } else if (pendingRefresh) {
            fetchRemote();
        }
    }
}
