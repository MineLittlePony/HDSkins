package com.minelittlepony.hdskins.client.resources;

import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

public class LocalTexture {
    private final SkinType type;
    private final IBlankSkinSupplier defaultTexture;

    private Optional<PreviewTextureManager.UriTexture> server = Optional.empty();
    private Optional<PreviewTextureManager.FileTexture> local = Optional.empty();

    public LocalTexture(SkinType type, IBlankSkinSupplier blank) {
        this.type = type;
        defaultTexture = blank;
    }

    public Identifier getId() {
        return getServerTexture()
                .map(a -> (PreviewTextureManager.Texture)a)
                .or(this::getLocalTexture)
                .map(PreviewTextureManager.Texture::getId)
                .orElseGet(this::getDefault);
    }

    public Identifier getDefault() {
        return defaultTexture.getBlankSkin(type);
    }

    public boolean hasLocalTexture() {
        return !server.isPresent() && local.isPresent();
    }

    public boolean uploadComplete() {
        return getServerTexture().isPresent();
    }

    public Optional<PreviewTextureManager.UriTexture> getServerTexture() {
        return server.filter(PreviewTextureManager.Texture::isLoaded);
    }

    public Optional<PreviewTextureManager.FileTexture> getLocalTexture() {
        return local.filter(PreviewTextureManager.Texture::isLoaded);
    }

    public void setRemote(PreviewTextureManager ptm, SkinCallback callback) {
        server.ifPresent(AbstractTexture::close);
        server = ptm.loadServerTexture(type, getDefault(), callback);
    }

    public void setLocal(Path file) throws IOException {
        local.ifPresent(AbstractTexture::close);

        try (InputStream input = Files.newInputStream(file)) {
            Identifier id = new Identifier("hdskins", "generated_preview/" + type.getPathName());
            PreviewTextureManager.FileTexture image = new PreviewTextureManager.FileTexture(HDPlayerSkinTexture.filterPlayerSkins(NativeImage.read(input)), id);

            MinecraftClient.getInstance().getTextureManager().registerTexture(id, image);

            local = Optional.of(image);
        }
    }

    public void dispose() {
        local.ifPresent(AbstractTexture::close);
        local = Optional.empty();
    }

    @FunctionalInterface
    public interface IBlankSkinSupplier {
        @Nullable
        Identifier getBlankSkin(SkinType type);
    }
}
