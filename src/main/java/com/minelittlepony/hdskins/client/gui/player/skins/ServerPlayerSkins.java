package com.minelittlepony.hdskins.client.gui.player.skins;

import java.util.*;
import java.util.function.*;

import com.minelittlepony.hdskins.client.resources.DynamicTextures;
import com.minelittlepony.hdskins.client.resources.Texture;
import com.minelittlepony.hdskins.client.resources.TextureLoader;
import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinServer;
import com.minelittlepony.hdskins.server.TexturePayload;

import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

public class ServerPlayerSkins extends PlayerSkins<ServerPlayerSkins.RemoteTexture> {

    private Optional<DynamicTextures> textureManager = Optional.empty();

    private Optional<SkinServer.SkinServerProfile<?>> profile = Optional.empty();
    private final Map<SkinType, List<Skin>> skinLists = new HashMap<>();

    public ServerPlayerSkins(Posture posture) {
        super(posture);
    }

    public void loadProfile(Optional<SkinServer.SkinServerProfile<?>> profile) {
        this.profile = profile;
        skinLists.clear();
    }

    public void loadTextures(TexturePayload payload, SkinCallback loadCallback) {
        super.close();
        textureManager = Optional.of(new DynamicTextures(payload, loadCallback));
    }

    @Override
    protected RemoteTexture createTexture(SkinType type, Supplier<Identifier> blank) {
        return new RemoteTexture(blank, textureManager.flatMap(tpm -> tpm.loadTexture(type, blank.get())));
    }

    public List<Skin> getProfileSkins(SkinType type) {
        return skinLists.computeIfAbsent(type, t -> {
            return profile.map(profile -> {
                return profile.getSkins(t).stream().map(skin -> {
                    String model = skin.getModel();
                    String uri = skin.getUri();
                    String hash = String.valueOf(uri.hashCode());

                    Identifier id = new Identifier("hdskins", String.format("dynamic/%s/%s", type.getId().getPath(), hash));
                    Supplier<Identifier> blank = () -> getPosture().getDefaultSkin(t, model);

                    return new Skin(blank, Optional.of(TextureLoader.loadTexture(id, Texture.UriTexture.create(id, DynamicTextures.createTempFile(hash), uri, type, model, blank.get(), null))), skin.isActive());
                }).toList();
            }).orElse(List.of());
        });
    }

    @Override
    public void close() {
        textureManager = Optional.empty();
        profile = Optional.empty();
        skinLists.clear();
        super.close();
    }

    @Override
    public String getSkinVariant() {
        return textureManager
                .flatMap(manager -> manager.getTextureMetadata(SkinType.SKIN))
                .map(metadata -> metadata.getMetadata("model"))
                .orElseGet(() -> DefaultSkinHelper.getSkinTextures(getPosture().getProfile().getId()).model().getName());
    }

    @Override
    protected boolean isProvided(SkinType type) {
        return get(type).isReady() || textureManager.flatMap(manager -> manager.getTextureMetadata(type)).isPresent();
    }

    public record Skin(
            Supplier<Identifier> blank,
            Optional<Texture.UriTexture> texture,
            boolean active) implements PlayerSkins.PlayerSkin {

        @Override
        public Identifier getId() {
            return texture != null ? blank.get() : texture.filter(Texture::isLoaded).map(Texture::getId).orElseGet(blank);
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
