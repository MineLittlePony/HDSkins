package com.minelittlepony.hdskins.client.dummy;

import com.minelittlepony.hdskins.client.resources.LocalTexture;
import com.minelittlepony.hdskins.client.resources.LocalTexture.IBlankSkinSupplier;
import com.minelittlepony.hdskins.client.resources.PreviewTextureManager;
import com.minelittlepony.hdskins.client.resources.SkinCallback;
import com.minelittlepony.hdskins.skins.SkinServer;
import com.mojang.authlib.GameProfile;

import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TextureProxy implements IBlankSkinSupplier {

    private final Map<Type, LocalTexture> textures = new HashMap<>();

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

    public GameProfile getProfile() {
        return profile;
    }

    @Override
    public Identifier getBlankSkin(Type type) {
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
//        if (textures.containsKey(Type.SKIN)) {
//            LocalTexture skin = get(Type.SKIN);
//
//            if (skin.uploadComplete()) {
//                return skin.getServerTexture()
//                        .filter(PreviewTextureManager.Texture::hasModel)
//                        .map(PreviewTextureManager.Texture::usesThinArms)
//                        .orElse(previewThinArms);
//            }
//        }

        return previewThinArms;
    }

    public CompletableFuture<Void> reloadRemoteSkin(SkinServer gateway, SkinCallback listener) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return new PreviewTextureManager(gateway.loadProfileData(MinecraftClient.getInstance().getSessionService(), profile));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, Util.getServerWorkerExecutor())
                .thenAcceptAsync(ptm -> {
                    for (Type type : Type.values()) {
                        get(type).setRemote(ptm, listener);
                    }
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

    public LocalTexture get(Type type) {
        return textures.computeIfAbsent(type, this::supplyNewTexture);
    }

    private LocalTexture supplyNewTexture(Type type) {
        return new LocalTexture(profile, type, this);
    }
}
