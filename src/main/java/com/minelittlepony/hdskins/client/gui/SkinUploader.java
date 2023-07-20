package com.minelittlepony.hdskins.client.gui;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.gui.player.DummyPlayer;
import com.minelittlepony.hdskins.client.gui.player.skins.ServerPlayerSkins;
import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Uploader contains form data and server communication logic.
 */
public class SkinUploader implements Closeable, CarouselStatusLabel {
    public static final Text STATUS_OK = ScreenTexts.EMPTY;
    public static final Text STATUS_NO_SERVER = Text.translatable("hdskins.error.noserver");
    public static final Text STATUS_OFFLINE = Text.translatable("hdskins.error.offline");
    public static final Text STATUS_SESSION = Text.translatable("hdskins.error.session.short");
    public static final Text ERR_SESSION = Text.translatable("hdskins.error.session");

    public static final Text STATUS_MOJANG = Text.translatable("hdskins.error.mojang");
    public static final Text STATUS_BUSY = Text.translatable("hdskins.status.busy");
    public static final String ERR_MOJANG_WAIT = "hdskins.error.mojang.wait";

    public static final Text STATUS_FETCH = Text.translatable("hdskins.fetch");

    private Text bannerMessage = STATUS_OK;

    private Map<String, String> skinMetadata = new HashMap<>();

    private volatile boolean pendingRefresh = false;

    private int reloadCounter = 0;
    private int retries = 1;

    private final DualCarouselWidget previewer;

    private final Iterator<Gateway> gateways;
    private Optional<Gateway> gateway;

    private SkinCallback loadListener = SkinCallback.NOOP;
    private Consumer<SkinType> skinTypeChangedListener = t -> {};

    private final SkinUpload.Session session;

    public SkinUploader(Iterator<Gateway> gateways, DualCarouselWidget previewer, SkinUpload.Session session) {
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
            pendingRefresh = true;
        } else {
            setBannerMessage(STATUS_NO_SERVER);
        }
    }

    public Optional<Gateway> getGateway() {
        return gateway;
    }

    public String getGatewayText() {
        return gateway.map(Gateway::getServer).map(SkinServer::toString).orElse("");
    }

    public Set<Feature> getFeatures() {
        return gateway.map(Gateway::getServer).map(SkinServer::getFeatures).orElse(Set.of());
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
                && getBannerMessage() != ERR_SESSION;
    }

    public boolean isBusy() {
        return gateway.filter(Gateway::isBusy).isPresent();
    }

    private boolean isSkinOperationsBlocked() {
        return !isOnline() || hasBannerMessage() || isBusy();
    }

    public boolean canUpload(SkinType type) {
        return getFeatures().contains(Feature.UPLOAD_USER_SKIN) && !isSkinOperationsBlocked() && previewer.getLocal().getSkins().get(type).isReady();
    }

    public boolean canClear(SkinType type) {
        return getFeatures().contains(Feature.DELETE_USER_SKIN) && !isSkinOperationsBlocked() && previewer.getRemote().getSkins().get(type).isReady();
    }

    public boolean canClearAny() {
        return getFeatures().contains(Feature.DELETE_USER_SKIN) && !isSkinOperationsBlocked() && previewer.getRemote().getSkins().hasAny();
    }

    public boolean hasBannerMessage() {
        return bannerMessage != STATUS_OK;
    }

    public Text getBannerMessage() {
        return bannerMessage;
    }

    public void setBannerMessage(Text er) {
        bannerMessage = er;
    }

    @Override
    public boolean hasStatus() {
        return getStatus() != STATUS_OK;
    }

    private Text getStatus() {
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
    public List<Text> getStatusLines() {
        Text status = getStatus();
        if (status == STATUS_MOJANG) {
            return List.of(status, Text.translatable(ERR_MOJANG_WAIT, getRetries()));
        }
        return List.of(status);
    }

    @Override
    public int getLabelColor(Text status) {
        return isThrottled() || status == STATUS_SESSION || status == STATUS_OFFLINE ? RED : WHITE;
    }

    public void setMetadataField(String field, String value) {
        previewer.getLocal().getSkins().close();
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

    public CompletableFuture<Void> uploadSkin(Text statusMsg, SkinUpload payload) {
        setBannerMessage(statusMsg);
        return gateway
                .map(g -> g.uploadSkin(payload, this::setBannerMessage))
                .map(future -> future.thenRunAsync(this::scheduleReload, MinecraftClient.getInstance()))
                .orElseGet(() -> CompletableFuture.failedFuture(new IOException("No gateway"))).whenComplete((o, t) -> {
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
                .fetchSkins(previewer.getProfile(), this::setBannerMessage)
                .thenAcceptAsync(textures -> {
                    ServerPlayerSkins skins = previewer.getRemote().getSkins();
                    skins.loadTextures(textures, loadListener);
                    gateway.getProfile(previewer.getProfile()).thenAccept(serverProfile -> {
                        skins.loadProfile(serverProfile);
                    });
                }, MinecraftClient.getInstance())
                .handleAsync((a, throwable) -> {
                    if (throwable == null) {
                        retries = 1;
                    }
                    return a;
                }, MinecraftClient.getInstance());
        });
    }

    @Override
    public void close() throws IOException {
        previewer.close();
    }

    public void update() {
        previewer.apply(DummyPlayer::updateModel);

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
