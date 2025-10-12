package com.minelittlepony.hdskins.client.ducks;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.PlayerSkins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.DefaultSkinHelper;

public interface ClientPlayerInfo {
    /**
     * Container for the player's skin types, metadata, and textures.
     */
    PlayerSkins getSkins();

    static Optional<ClientPlayerInfo> of(@Nullable AbstractClientPlayerEntity player) {
        return player == null ? Optional.empty() : of(player.getUuid());
    }

    static Optional<ClientPlayerInfo> of(UUID playerId) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        return networkHandler == null ? Optional.empty() : Optional.ofNullable(networkHandler.getPlayerListEntry(playerId)).map(entry -> {
            var defaultSkins = DefaultSkinHelper.getSkinTextures(entry.getProfile());
            var skinsFuture = client.getSkinProvider().fetchSkinTextures(entry.getProfile());
            return PlayerSkins.create(entry.getProfile(), () -> {
                return skinsFuture.getNow(Optional.empty()).orElse(defaultSkins);
            })::get;
        });
    }
}
