package com.minelittlepony.hdskins.client.dummy;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.minelittlepony.hdskins.client.SkinUploader;
import com.minelittlepony.hdskins.skins.SkinType;
import com.minelittlepony.hdskins.client.resources.LocalTexture;
import com.minelittlepony.hdskins.client.resources.PreviewTextureManager;
import com.minelittlepony.hdskins.client.resources.SkinCallback;
import com.minelittlepony.hdskins.client.resources.LocalTexture.IBlankSkinSupplier;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

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
        if (!isUsingLocal() && !this.isUsingRemote()) {
            textures.clear();
        }
    }

    public boolean usesThinSkin() {
        if (textures.containsKey(SkinType.SKIN)) {
            LocalTexture skin = get(SkinType.SKIN);

            if (skin.uploadComplete() && skin.getRemote().hasModel()) {
                return skin.getRemote().usesThinArms();
            }
        }

        return previewThinArms;
    }

    public CompletableFuture<Void> reloadRemoteSkin(SkinUploader uploader, SkinCallback listener) {
        return uploader.getGateway().getPreviewTextures(profile)
                .thenApply(PreviewTextureManager::new)
                .thenAcceptAsync(ptm -> {
            SkinType.values().forEach(type -> get(type).setRemote(ptm, listener));
        }, MinecraftClient.getInstance()::execute); // run on main thread
    }

    public void setLocal(Path skinTextureFile, SkinType type) {
        get(type).setLocal(skinTextureFile);
    }

    public boolean isSetupComplete() {
        return textures.values().stream().allMatch(LocalTexture::uploadComplete);
    }

    public boolean isUsingLocal() {
        return textures.values().stream().anyMatch(LocalTexture::usingLocal);
    }

    public boolean isUsingRemote() {
        return textures.values().stream().anyMatch(LocalTexture::hasRemoteTexture);
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
