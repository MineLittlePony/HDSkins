package com.minelittlepony.hdskins.client.resources;

import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
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

    private final Identifier remoteResource;
    private Optional<PreviewTextureManager.Texture> server = Optional.empty();

    private boolean isDynamic;
    private Identifier localResource;

    private final IBlankSkinSupplier blank;

    private final SkinType type;

    public LocalTexture(GameProfile profile, SkinType type, IBlankSkinSupplier blank) {
        this.blank = blank;
        this.type = type;

        localResource = blank.getBlankSkin(type);
        remoteResource = new Identifier(String.format("%s/preview_%s", type.name(), profile.getName()).toLowerCase());
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
            isDynamic = true;
        }
    }

    public void clearRemote() {
        server = server.map(server -> {
            textureManager.destroyTexture(remoteResource);
            return null;
        });
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
        Identifier getBlankSkin(SkinType type);
    }
}
