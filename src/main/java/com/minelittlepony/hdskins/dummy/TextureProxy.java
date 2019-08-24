package com.minelittlepony.hdskins.dummy;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.minelittlepony.hdskins.SkinUploader;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.resources.LocalTexture;
import com.minelittlepony.hdskins.resources.PreviewTextureManager;
import com.minelittlepony.hdskins.resources.SkinCallback;
import com.minelittlepony.hdskins.resources.LocalTexture.IBlankSkinSupplier;
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

    private final IBlankSkinSupplier blankSupplier;

    TextureProxy(GameProfile gameprofile, IBlankSkinSupplier blank) {
        profile = gameprofile;
        blankSupplier = blank;
    }

    @Override
    public Identifier getBlankSkin(SkinType type) {
        return blankSupplier.getBlankSkin(type);
    }

    public void setPose(int pose) {
        previewSleeping = pose == 1;
        previewRiding = pose == 2;
        previewSwimming = pose == 3;
    }

    public void setPreviewThinArms(boolean thinArms) {
        previewThinArms = thinArms;
    }

    public boolean usesThinSkin() {
        LocalTexture skin = get(SkinType.SKIN);

        if (skin.uploadComplete() && skin.getRemote().hasModel()) {
            return skin.getRemote().usesThinArms();
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
