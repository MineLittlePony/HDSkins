package com.minelittlepony.hdskins.client.ducks;

import com.minelittlepony.hdskins.client.PlayerSkins;
import com.minelittlepony.hdskins.mixin.client.MixinClientPlayer;

import net.minecraft.client.network.AbstractClientPlayerEntity;

public interface ClientPlayerInfo {
    /**
     * Container for the player's skin types, metadata, and textures.
     */
    PlayerSkins getSkins();

    static ClientPlayerInfo of(AbstractClientPlayerEntity player) {
        return (ClientPlayerInfo)((MixinClientPlayer)player).getBackingClientData();
    }
}
