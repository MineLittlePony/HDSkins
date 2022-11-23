package com.minelittlepony.hdskins.client.resources;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.dummy.PlayerSkins;
import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinServer;
import com.mojang.authlib.exceptions.AuthenticationException;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ServerPlayerSkins extends PlayerSkins<ServerPlayerSkins.RemoteTexture> {
    public ServerPlayerSkins(Posture posture) {
        super(posture, RemoteTexture::new);
    }

    public CompletableFuture<Void> reloadRemoteSkin(SkinServer gateway, SkinCallback listener) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new PreviewTextureManager(gateway.loadProfileData(posture.getProfile()));
            } catch (IOException | AuthenticationException e) {
                throw new RuntimeException(e);
            }
        }, Util.getMainWorkerExecutor()).thenAcceptAsync(ptm -> {
            SkinType.REGISTRY.stream()
                .filter(t -> t != SkinType.UNKNOWN)
                .forEach(type -> get(type).setRemote(ptm, listener));
        }, MinecraftClient.getInstance()); // run on main thread
    }

    @Override
    public boolean usesThinSkin() {
        if (textures.containsKey(SkinType.SKIN)) {
            RemoteTexture skin = get(SkinType.SKIN);

            if (skin.isReady()) {
                return skin.getServerTexture()
                        .filter(PreviewTextureManager.UriTexture::hasModel)
                        .map(PreviewTextureManager.UriTexture::usesThinArms)
                        .orElse(VanillaModels.isSlim(DefaultSkinHelper.getModel(posture.getProfile().getId())));
            }
        }

        return VanillaModels.isSlim(DefaultSkinHelper.getModel(posture.getProfile().getId()));
    }

    public static class RemoteTexture implements PlayerSkins.PlayerSkin {
        private final SkinType type;
        private final Supplier<Identifier> defaultTexture;

        private Optional<PreviewTextureManager.UriTexture> server = Optional.empty();

        public RemoteTexture(SkinType type, Supplier<Identifier> blank) {
            this.type = type;
            defaultTexture = blank;
        }

        @Override
        public Identifier getId() {
            return getServerTexture()
                    .map(a -> (PreviewTextureManager.Texture)a)
                    .map(PreviewTextureManager.Texture::getId)
                    .orElseGet(defaultTexture);
        }

        @Override
        public boolean isReady() {
            return getServerTexture().isPresent();
        }

        public Optional<PreviewTextureManager.UriTexture> getServerTexture() {
            return server.filter(PreviewTextureManager.Texture::isLoaded);
        }

        public void setRemote(PreviewTextureManager ptm, SkinCallback callback) {
            server.ifPresent(AbstractTexture::close);
            server = ptm.loadServerTexture(type, defaultTexture.get(), callback);
        }

        @Override
        public void close() {
            server.ifPresent(AbstractTexture::close);
            server = Optional.empty();
        }
    }
}
