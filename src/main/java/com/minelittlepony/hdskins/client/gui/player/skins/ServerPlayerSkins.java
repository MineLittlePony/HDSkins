package com.minelittlepony.hdskins.client.gui.player.skins;

import java.util.*;
import java.util.function.*;

import com.minelittlepony.hdskins.client.resources.DynamicTextures;
import com.minelittlepony.hdskins.client.resources.Texture;
import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinServer;
import com.minelittlepony.hdskins.server.TexturePayload;

import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

public class ServerPlayerSkins extends PlayerSkins<ServerPlayerSkins.RemoteTexture> {

    private Optional<DynamicTextures> textureManager = Optional.empty();

    private Optional<? extends SkinServer.SkinServerProfile<?>> profile = Optional.empty();
    private final Map<SkinType, List<PreviousServerPlayerSkins>> skinLists = new HashMap<>();

    public ServerPlayerSkins(Posture posture) {
        super(posture);
    }

    public void loadProfile(Optional<? extends SkinServer.SkinServerProfile<?>> profile) {
        this.profile = profile;
        skinLists.clear();
    }

    public void loadTextures(TexturePayload payload, SkinCallback loadCallback) {
        super.close();
        textureManager = Optional.of(new DynamicTextures(payload, loadCallback));
    }

    @Override
    protected RemoteTexture createTexture(SkinType type, Supplier<Identifier> blank) {
        return new RemoteTexture(blank, textureManager.flatMap(tpm -> tpm.loadTexture(type, blank.get())), true);
    }

    public List<PreviousServerPlayerSkins> getProfileSkins(SkinType type) {
        return skinLists.computeIfAbsent(type, t -> profile
                .map(profile -> profile.getSkins(t).stream()
                    .map(skin -> new PreviousServerPlayerSkins(this, skin, type)).toList())
                .orElse(List.of()));
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

    public record RemoteTexture (
            Supplier<Identifier> blank,
            Optional<Texture.UriTexture> texture,
            boolean active
    ) implements PlayerSkins.PlayerSkin {

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
