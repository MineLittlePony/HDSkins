package com.minelittlepony.hdskins.client.resources;

import java.util.Optional;
import java.util.function.*;

import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.dummy.PlayerSkins;
import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.TexturePayload;

import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

public class ServerPlayerSkins extends PlayerSkins<ServerPlayerSkins.RemoteTexture> {

    private Optional<DynamicTextures> textureManager = Optional.empty();

    public ServerPlayerSkins(Posture posture) {
        super(posture);
    }

    public void setSkins(TexturePayload payload, SkinCallback loadCallback) {
        close();
        textureManager = Optional.of(new DynamicTextures(payload, loadCallback));
    }

    @Override
    protected RemoteTexture createTexture(SkinType type, Supplier<Identifier> blank) {
        return new RemoteTexture(blank, textureManager.flatMap(tpm -> tpm.loadTexture(type, blank.get())));
    }

    @Override
    public void close() {
        textureManager = Optional.empty();
        super.close();
    }

    @Override
    public boolean usesThinSkin() {
        return textureManager
                .flatMap(manager -> manager.getTextureMetadata(SkinType.SKIN))
                .map(metadata -> metadata.getMetadata("model"))
                .map(VanillaModels::isSlim)
                .orElseGet(() -> VanillaModels.isSlim(DefaultSkinHelper.getModel(posture.getProfile().getId())));
    }

    public record RemoteTexture (
            Supplier<Identifier> blank,
            Optional<Texture.UriTexture> texture) implements PlayerSkins.PlayerSkin {

        @Override
        public Identifier getId() {
            return texture.filter(Texture::isLoaded).map(Texture::getId).orElseGet(blank);
        }

        @Override
        public boolean isReady() {
            return texture.filter(Texture::isLoaded).isPresent();
        }

        @Override
        public void close() {
            texture.ifPresent(Texture.UriTexture::close);
        }
    }
}
