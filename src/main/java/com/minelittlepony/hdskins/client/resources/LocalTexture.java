package com.minelittlepony.hdskins.client.resources;

import com.minelittlepony.hdskins.skins.SkinType;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;

public class LocalTexture {

    private final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();

    @Nullable
    private NativeImageBackedTexture local;

    @Nullable
    private PreviewTexture remote;

    private Identifier remoteResource;
    private Identifier localResource;

    private final IBlankSkinSupplier blank;

    private final SkinType type;

    private boolean remoteLoaded = false;

    public LocalTexture(GameProfile profile, SkinType type, IBlankSkinSupplier blank) {
        this.blank = blank;
        this.type = type;

        String file = String.format("%s/preview_%s", type.name(), profile.getName()).toLowerCase();

        remoteResource = new Identifier(file);
        textureManager.destroyTexture(remoteResource);


        reset();
    }

    public Identifier getId() {
        if (hasRemote()) {
            return remoteResource;
        }

        return localResource;
    }

    public void reset() {
        localResource = blank.getBlankSkin(type);
    }

    public boolean hasRemote() {
        return remote != null;
    }

    public boolean hasLocal() {
        return local != null;
    }

    public boolean hasRemoteTexture() {
        return uploadComplete() && remoteLoaded;
    }

    public boolean usingLocal() {
        return !hasRemote() && hasLocal();
    }

    public boolean uploadComplete() {
        return hasRemote() && remote.isTextureUploaded();
    }

    @Nullable
    public PreviewTexture getRemote() {
        return remote;
    }

    public void setRemote(PreviewTextureManager ptm, SkinCallback callback) {
        clearRemote();

        Identifier blank = this.blank.getBlankSkin(type);

        if (blank != null) {
            remote = ptm.getPreviewTexture(remoteResource, type, blank, callback.andThen(() -> remoteLoaded = true));
        }
    }

    public void setLocal(Path file) {

        clearLocal();

        if (!Files.exists(file)) {
            return;
        }

        try (InputStream input = Files.newInputStream(file)) {
            NativeImage image = HDPlayerSkinTexture.filterPlayerSkins(NativeImage.read(input));

            local = new NativeImageBackedTexture(image);
            localResource = textureManager.registerDynamicTexture("local_skin_preview", local);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearRemote() {
        remoteLoaded = false;
        if (hasRemote()) {
            remote = null;
            textureManager.destroyTexture(remoteResource);
        }
    }

    public void clearLocal() {
        if (hasLocal()) {
            local = null;
            textureManager.destroyTexture(localResource);
            localResource = blank.getBlankSkin(type);
        }
    }

    public interface IBlankSkinSupplier {

        Identifier getBlankSkin(SkinType type);
    }
}
