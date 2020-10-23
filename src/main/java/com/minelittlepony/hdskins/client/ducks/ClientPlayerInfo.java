package com.minelittlepony.hdskins.client.ducks;

import java.util.Map;

import com.minelittlepony.hdskins.client.PlayerSkins;
import com.minelittlepony.hdskins.mixin.client.MixinClientPlayer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;

public interface ClientPlayerInfo {

    /**
     * The vanilla textures, provided as-is from Mojang.
     */
    Map<Type, Identifier> getVanillaTextures();

    /**
     * The game profile
     */
    GameProfile getGameProfile();

    /**
     * Container for the player's skin types, metadata, and textures.
     */
    PlayerSkins getSkins();

    static ClientPlayerInfo of(ClientPlayerEntity player) {
        return (ClientPlayerInfo)((MixinClientPlayer)player).getBackingClientData();
    }
}
