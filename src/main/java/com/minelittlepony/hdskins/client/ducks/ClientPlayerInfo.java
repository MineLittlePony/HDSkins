package com.minelittlepony.hdskins.client.ducks;

import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.PlayerSkins;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;

public interface ClientPlayerInfo {
    /**
     * Container for the player's skin types, metadata, and textures.
     */
    PlayerSkins getSkins();

    static Optional<ClientPlayerInfo> of(@Nullable AbstractClientPlayerEntity player) {
        return player == null ? Optional.empty() : of(player.getGameProfile());
    }

    static Optional<ClientPlayerInfo> of(@Nullable PlayerLikeEntity player) {
        if (player instanceof PlayerEntity p) {
            return of(p.getGameProfile());
        }
        return of(player.get(DataComponentTypes.PROFILE));
    }

    static Optional<ClientPlayerInfo> of(@Nullable GameProfile profile) {
        return profile == null ? Optional.empty() : of(ProfileComponent.ofStatic(profile));
    }

    static Optional<ClientPlayerInfo> of(@Nullable ProfileComponent profile) {
        MinecraftClient client = MinecraftClient.getInstance();
        return profile == null ? Optional.empty() : Optional.of((ClientPlayerInfo)(Object)client.getPlayerSkinCache().get(profile));
    }

    static Optional<ClientPlayerInfo> of(@Nullable UUID playerId) {
        return playerId == null ? Optional.empty() : of(ProfileComponent.ofDynamic(playerId));
    }
}
