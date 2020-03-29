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
import java.util.Optional;

import javax.annotation.Nullable;

public class LocalTexture {

    private final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();

    private Optional<NativeImageBackedTexture> local = Optional.empty();
    private Optional<PreviewTextureManager.Texture> server = Optional.empty();

    private Identifier localResource;
    private Identifier remoteResource;

    private final IBlankSkinSupplier blank;

    private final SkinType type;

    public LocalTexture(GameProfile profile, SkinType type, IBlankSkinSupplier blank) {
        this.blank = blank;
        this.type = type;

        String file = String.format("%s/preview_%s", type.name(), profile.getName()).toLowerCase();

        localResource = blank.getBlankSkin(type);

        remoteResource = new Identifier(file);
        textureManager.destroyTexture(remoteResource);
    }

    public Identifier getId() {
        if (server.isPresent()) {
            return remoteResource;
        }

        return localResource;
    }

    public boolean hasServerTexture() {
        return uploadComplete();
    }

    public boolean hasLocalTexture() {
        return !server.isPresent() && local.isPresent();
    }

    public boolean uploadComplete() {
        return server.map(PreviewTextureManager.Texture::isLoaded).orElse(false);
    }

    public Optional<PreviewTextureManager.Texture> getServerTexture() {
        return server;
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
        }
    }

    public void clearRemote() {
        server.ifPresent(server -> textureManager.destroyTexture(remoteResource));
        server = Optional.empty();
    }

    public void clearLocal() {
        local.ifPresent(local -> {
            textureManager.destroyTexture(localResource);
            localResource = blank.getBlankSkin(type);
        });
        local = Optional.empty();
    }

    @FunctionalInterface
    public interface IBlankSkinSupplier {
        @Nullable
        Identifier getBlankSkin(SkinType type);
    }
}
