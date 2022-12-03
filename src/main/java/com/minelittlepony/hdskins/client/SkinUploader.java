package com.minelittlepony.hdskins.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.dummy.DummyPlayer;
import com.minelittlepony.hdskins.client.dummy.PlayerPreview;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.*;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

    private final PlayerPreview previewer;

    private final Iterator<Gateway> gateways;
    private Optional<Gateway> gateway;

    private final SkinChangeListener listener;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public SkinUploader(SkinServerList servers, PlayerPreview previewer, SkinChangeListener listener) {
        this.previewer = previewer;
        this.listener = listener;

        skinMetadata.put("model", VanillaModels.DEFAULT);
        gateways = servers.getCycler();

        cycleGateway();
    }

    public void cycleGateway() {
        if (gateways.hasNext()) {
            gateway = Optional.ofNullable(gateways.next());
            setSkinType(gateway.flatMap(g -> g.getServer().supportsSkinType(previewer.getActiveSkinType()) ? Optional.of(previewer.getActiveSkinType()) : getSupportedSkinTypes().findFirst()).orElse(SkinType.UNKNOWN));
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
        listener.onSkinTypeChanged(type);
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

    public boolean canUpload() {
        return isOnline()
                && !hasBannerMessage()
                && !isBusy()
                && previewer.getClientTextures().isSetupComplete();
    }

    public boolean canClear() {
        return isOnline()
                && !hasBannerMessage()
                && !isBusy()
                && previewer.getServerTextures().isSetupComplete();
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
        previewer.getClientTextures().close();
        skinMetadata.put(field, value);
    }

    public String getMetadataField(String field) {
        return skinMetadata.getOrDefault(field, "");
    }

    public boolean tryClearStatus() {
        if (!hasBannerMessage() || (!isBusy() || isThrottled())) {
            setBannerMessage(STATUS_OK);
            return true;
        }

        return false;
    }

    public CompletableFuture<Void> uploadSkin(Text statusMsg, @Nullable URI skin) {
        setBannerMessage(statusMsg);
        return gateway
                .map(g -> g.uploadSkin(new SkinUpload(mc.getSession(), previewer.getActiveSkinType(), skin, skinMetadata), this::setBannerMessage))
                .map(future -> future.thenRunAsync(this::scheduleReload, MinecraftClient.getInstance()))
                .orElseGet(() -> CompletableFuture.failedFuture(new IOException("No gateway")));
    }

    public void scheduleReload() {
        pendingRefresh = true;
    }

    protected void fetchRemote() {
        pendingRefresh = false;
        gateway.ifPresent(gateway -> {
            gateway.fetchSkins(previewer.getProfile(), this::setBannerMessage)
                .thenAcceptAsync(textures -> {
                    previewer.getServerTextures().setSkins(textures, listener::onSetRemoteSkin);

                    gateway.getProfile(previewer.getProfile()).thenAccept(serverProfile -> {
                        serverProfile.ifPresent(p -> {
                            previewer.getServerTextures().setSkinList(p);
                        });
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

    public interface SkinChangeListener {
        default void onSetRemoteSkin(SkinType type, Identifier location, MinecraftProfileTexture profileTexture) {}

        default void onSetLocalSkin(SkinType type) {}

        default void onSkinTypeChanged(SkinType newType) {}
    }
}
