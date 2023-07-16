package com.minelittlepony.hdskins.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.minelittlepony.hdskins.client.gui.DualPreview;
import com.minelittlepony.hdskins.client.gui.player.DummyPlayer;
import com.minelittlepony.hdskins.client.gui.player.skins.ServerPlayerSkins;
import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class SkinUploader implements Closeable {
    public static final Text STATUS_OK = ScreenTexts.EMPTY;
    public static final Text STATUS_NO_SERVER = Text.translatable("hdskins.error.noserver");
    public static final Text STATUS_OFFLINE = Text.translatable("hdskins.error.offline");
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

    private final DualPreview previewer;

    private final Iterator<Gateway> gateways;
    private Optional<Gateway> gateway;

    private SkinCallback uploadListener = SkinCallback.NOOP;
    private Consumer<SkinType> skinTypeChangedListener = t -> {};

    public SkinUploader(Iterator<Gateway> gateways, DualPreview previewer) {
        this.previewer = previewer;
        this.gateways = gateways;
        skinMetadata.put("model", VanillaModels.DEFAULT);
        cycleGateway();
    }

    public void addSkinUploadedEventListener(SkinCallback listener) {
        this.uploadListener = this.uploadListener.andThen(listener);
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
        return gateway.filter(Gateway::isOnline).isPresent() || getBannerMessage() == ERR_SESSION;
    }

    public boolean isBusy() {
        return gateway.filter(Gateway::isBusy).isPresent();
    }

    private boolean isSkinOperationsBlocked() {
        return !getFeatures().contains(Feature.UPLOAD_USER_SKIN) || !isOnline() || hasBannerMessage() || isBusy();
    }

    public boolean canUpload(SkinType type) {
        return !isSkinOperationsBlocked() && previewer.getLocal().getSkins().get(type).isReady();
    }

    public boolean canClear(SkinType type) {
        return !isSkinOperationsBlocked() && previewer.getRemote().getSkins().get(type).isReady();
    }

    public boolean canClearAny() {
        return !isSkinOperationsBlocked() && previewer.getRemote().getSkins().hasAny();
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

    public boolean hasStatus() {
        return getStatus() != STATUS_OK;
    }

    public Text getStatus() {
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

        return STATUS_OK;
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
                .orElseGet(() -> CompletableFuture.failedFuture(new IOException("No gateway")));
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
                    skins.loadTextures(textures, uploadListener);
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
