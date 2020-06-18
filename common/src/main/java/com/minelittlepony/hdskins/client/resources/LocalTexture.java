package com.minelittlepony.hdskins.client.resources;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nullable;

public class LocalTexture {

    private final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();

    private Optional<NativeImageBackedTexture> local = Optional.empty();

    private final Identifier remoteResource;
    private AbstractTexture server;

    private boolean isDynamic;
    private Identifier localResource;

    private final IBlankSkinSupplier blank;

    private final Type type;

    public LocalTexture(GameProfile profile, Type type, IBlankSkinSupplier blank) {
        this.blank = blank;
        this.type = type;

        localResource = blank.getBlankSkin(type);
        remoteResource = new Identifier(String.format("%s/preview_%s", type.name(), profile.getName()).toLowerCase());
    }

    public Identifier getId() {
        if (server != null) {
            return remoteResource;
        }

        return localResource;
    }

    public boolean hasServerTexture() {
        return uploadComplete();
    }

    public boolean hasLocalTexture() {
        return server != null && local.isPresent();
    }

    public boolean uploadComplete() {
        return false;
//        return getServerTexture().map(PreviewTextureManager.Texture::isLoaded).orElse(false);
    }

    public Optional<AbstractTexture> getServerTexture() {
        return Optional.ofNullable(server);
    }

    public void setRemote(PreviewTextureManager ptm, SkinCallback callback) {
        clearRemote();

        Identifier blank = this.blank.getBlankSkin(type);

        if (blank != null) {
            server = ptm.loadServerTexture(remoteResource, type, blank, callback);
        }
    }

    public void setLocal(Path file) throws IOException {
        clearLocal();

        try (InputStream input = Files.newInputStream(file)) {
            NativeImage image = HDPlayerSkinTexture.filterPlayerSkins(NativeImage.read(input));

            local = Optional.of(new NativeImageBackedTexture(image));
            localResource = textureManager.registerDynamicTexture("local_skin_preview", local.get());
            isDynamic = true;
        }
    }

    public void clearRemote() {
        if (server != null) {
            textureManager.destroyTexture(remoteResource);
            server = null;
        }
    }

    public void clearLocal() {
        local = local.map(local -> {
            if (isDynamic) {
                textureManager.destroyTexture(localResource);
            }
            isDynamic = false;
            localResource = blank.getBlankSkin(type);
            return null;
        });
    }

    @FunctionalInterface
    public interface IBlankSkinSupplier {
        @Nullable
        Identifier getBlankSkin(Type type);
    }
}
