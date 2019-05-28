package com.minelittlepony.hdskins.dummy;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import com.minelittlepony.hdskins.SkinUploader;
import com.minelittlepony.hdskins.resources.LocalTexture;
import com.minelittlepony.hdskins.resources.LocalTexture.IBlankSkinSupplier;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinProvider.SkinTextureAvailableCallback;
import net.minecraft.util.Identifier;

public class TextureProxy implements IBlankSkinSupplier {

    private final LocalTexture skin;
    private final LocalTexture elytra;

    private final GameProfile profile;

    protected boolean previewThinArms = false;
    protected boolean previewSleeping = false;
    protected boolean previewRiding = false;
    
    private final IBlankSkinSupplier blankSupplier;

    TextureProxy(GameProfile gameprofile, IBlankSkinSupplier blank) {
        profile = gameprofile;
        blankSupplier = blank;
        
        skin = new LocalTexture(profile, Type.SKIN, blankSupplier);
        elytra = new LocalTexture(profile, Type.ELYTRA, blankSupplier);
    }
    
    @Override
    public Identifier getBlankSkin(Type type) {
        return blankSupplier.getBlankSkin(type);
    }

    public void setSleeping(boolean sleep) {
        previewSleeping = sleep;
    }

    public void setRiding(boolean ride) {
        previewRiding = ride;
    }

    public void setPreviewThinArms(boolean thinArms) {
        previewThinArms = thinArms;
    }

    public boolean usesThinSkin() {
        if (skin.uploadComplete() && skin.getRemote().hasModel()) {
            return skin.getRemote().usesThinArms();
        }

        return previewThinArms;
    }

    public CompletableFuture<Void> reloadRemoteSkin(SkinUploader uploader, SkinTextureAvailableCallback listener) {
        return uploader.loadTextures(profile).thenAcceptAsync(ptm -> {
            skin.setRemote(ptm, listener);
            elytra.setRemote(ptm, listener);
        }, MinecraftClient.getInstance()::execute); // run on main thread
    }

    public void setLocal(Path skinTextureFile, Type type) {
        if (type == Type.SKIN) {
            skin.setLocal(skinTextureFile);
        } else if (type == Type.ELYTRA) {
            elytra.setLocal(skinTextureFile);
        }
    }

    public boolean isSetupComplete() {
        return skin.uploadComplete() && elytra.uploadComplete();
    }

    public boolean isUsingLocal() {
        return skin.usingLocal() || elytra.usingLocal();
    }

    public boolean isUsingRemote() {
        return skin.hasRemoteTexture() || elytra.hasRemoteTexture();
    }

    public void release() {
        skin.clearLocal();
        elytra.clearLocal();
    }

    public LocalTexture get(Type type) {
        return type == Type.SKIN ? skin : elytra;
    }
}
