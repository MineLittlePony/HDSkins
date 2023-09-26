package com.minelittlepony.hdskins.client.ducks;

import java.util.Optional;

import com.minelittlepony.hdskins.client.PlayerSkins;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public interface ClientPlayerInfo {
    /**
     * Container for the player's skin types, metadata, and textures.
     */
    PlayerSkins getSkins();

    static Optional<ClientPlayerInfo> of(AbstractClientPlayerEntity player) {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        return networkHandler == null ? Optional.empty() : Optional.ofNullable((ClientPlayerInfo)networkHandler.getPlayerListEntry(player.getUuid()));
    }
}
