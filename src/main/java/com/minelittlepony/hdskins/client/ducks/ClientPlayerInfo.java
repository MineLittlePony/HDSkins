package com.minelittlepony.hdskins.client.ducks;

import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.PlayerSkins;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;

public interface ClientPlayerInfo {
    /**
     * Container for the player's skin types, metadata, and textures.
     */
    PlayerSkins getSkins();

    static Optional<ClientPlayerInfo> of(@Nullable AbstractClientPlayer player) {
        return player == null ? Optional.empty() : of(player.getGameProfile());
    }

    static Optional<ClientPlayerInfo> of(@Nullable Avatar player) {
        if (player instanceof Player p) {
            return of(p.getGameProfile());
        }
        return of(player.get(DataComponents.PROFILE));
    }

    static Optional<ClientPlayerInfo> of(@Nullable GameProfile profile) {
        return profile == null ? Optional.empty() : of(ResolvableProfile.createResolved(profile));
    }

    static Optional<ClientPlayerInfo> of(@Nullable ResolvableProfile profile) {
        Minecraft client = Minecraft.getInstance();
        return profile == null ? Optional.empty() : Optional.of((ClientPlayerInfo)(Object)client.playerSkinRenderCache().getOrDefault(profile));
    }

    static Optional<ClientPlayerInfo> of(@Nullable UUID playerId) {
        return playerId == null ? Optional.empty() : of(ResolvableProfile.createUnresolved(playerId));
    }
}
