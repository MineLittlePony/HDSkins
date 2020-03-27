package com.minelittlepony.hdskins.client.dummy;

import com.minelittlepony.hdskins.client.resources.LocalTexture;
import com.minelittlepony.hdskins.client.resources.LocalTexture.IBlankSkinSupplier;
import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinServer;
import com.minelittlepony.hdskins.client.resources.PreviewTextureManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TextureProxy implements IBlankSkinSupplier {

    private final Map<SkinType, LocalTexture> textures = new HashMap<>();

    private final GameProfile profile;

    protected boolean previewThinArms = false;
    protected boolean previewSleeping = false;
    protected boolean previewRiding = false;
    protected boolean previewSwimming = false;

    private final IBlankSkinSupplier steveBlankSupplier;
    private final IBlankSkinSupplier alexBlankSupplier;

    TextureProxy(GameProfile gameprofile, IBlankSkinSupplier steve, IBlankSkinSupplier alex) {
        profile = gameprofile;
        steveBlankSupplier = steve;
        alexBlankSupplier = alex;
    }

    @Override
    public Identifier getBlankSkin(SkinType type) {
        if (usesThinSkin()) {
            return alexBlankSupplier.getBlankSkin(type);
        }
        return steveBlankSupplier.getBlankSkin(type);
    }

    public void setPose(int pose) {
        previewSleeping = pose == 1;
        previewRiding = pose == 2;
        previewSwimming = pose == 3;
    }

    public void setPreviewThinArms(boolean thinArms) {
        previewThinArms = thinArms;
        if (!isUsingLocal() && !isUsingRemote()) {
            textures.clear();
        }
    }

    public boolean usesThinSkin() {
        if (textures.containsKey(SkinType.SKIN)) {
            LocalTexture skin = get(SkinType.SKIN);

            if (skin.uploadComplete()) {
                return skin.getServerTexture()
                        .filter(PreviewTextureManager.Texture::hasModel)
                        .map(PreviewTextureManager.Texture::usesThinArms)
                        .orElse(previewThinArms);
            }
        }

        return previewThinArms;
    }

    public CompletableFuture<Void> reloadRemoteSkin(SkinServer gateway, SkinCallback listener) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return new PreviewTextureManager(gateway.loadProfileData(profile));
                    } catch (IOException | AuthenticationException e) {
                        throw new RuntimeException(e);
                    }
                }, Util.getServerWorkerExecutor())
                .thenAcceptAsync(ptm -> {
                    SkinType.values().forEach(type -> get(type).setRemote(ptm, listener));
                }, MinecraftClient.getInstance()); // run on main thread
    }

    public boolean isSetupComplete() {
        return textures.values().stream().allMatch(LocalTexture::uploadComplete);
    }

    public boolean isUsingLocal() {
        return textures.values().stream().anyMatch(LocalTexture::hasLocalTexture);
    }

    public boolean isUsingRemote() {
        return textures.values().stream().anyMatch(LocalTexture::hasServerTexture);
    }

    public void release() {
        textures.values().forEach(LocalTexture::clearLocal);
    }

    public LocalTexture get(SkinType type) {
        return textures.computeIfAbsent(type, this::supplyNewTexture);
    }

    private LocalTexture supplyNewTexture(SkinType type) {
        return new LocalTexture(profile, type, this);
    }
}
