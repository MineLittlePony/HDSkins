package com.minelittlepony.hdskins.resources;

import com.minelittlepony.hdskins.resources.texture.ImageBufferDownloadHD;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.PlayerSkinProvider.SkinTextureAvailableCallback;
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

    private final Type type;

    private boolean remoteLoaded = false;

    public LocalTexture(GameProfile profile, Type type, IBlankSkinSupplier blank) {
        this.blank = blank;
        this.type = type;

        String file = String.format("%s/preview_%s", type.name(), profile.getName()).toLowerCase();

        remoteResource = new Identifier(file);
        textureManager.destroyTexture(remoteResource);


        reset();
    }

    public Identifier getTexture() {
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

    public void setRemote(PreviewTextureManager ptm, SkinTextureAvailableCallback callback) {
        clearRemote();

        remote = ptm.getPreviewTexture(remoteResource, type, blank.getBlankSkin(type), (type, location, profileTexture) -> {
            if (callback != null) {
                callback.onSkinTextureAvailable(type, location, profileTexture);
            }
            remoteLoaded = true;
        });
    }

    public void setLocal(Path file) {
        if (!Files.exists(file)) {
            return;
        }

        clearLocal();

        try (InputStream input = Files.newInputStream(file)) {
            NativeImage image = new ImageBufferDownloadHD().parseUserSkin(NativeImage.fromInputStream(input));

            local = new NativeImageBackedTexture(image);
            localResource = textureManager.registerDynamicTexture("localSkinPreview", local);
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

        Identifier getBlankSkin(Type type);
    }
}
