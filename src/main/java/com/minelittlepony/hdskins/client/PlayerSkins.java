package com.minelittlepony.hdskins.client;

import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.minelittlepony.hdskins.client.ducks.ClientPlayerInfo;
import com.mojang.authlib.GameProfile;

import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;

public record PlayerSkins(PlayerSkinLayers layers, PlayerSkinLayers.Layer sorted) implements Supplier<PlayerSkin> {
    @Override
    public PlayerSkin get() {
        return sorted().getSkinTextures();
    }

    public static ClientPlayerInfo create(GameProfile profile, Supplier<PlayerSkin> texturesSupplier) {
        PlayerSkinLayers layers = PlayerSkinLayers.of(profile, texturesSupplier);
        return Suppliers.memoize(() -> new PlayerSkins(
                layers,
                new PlayerSkinLayers.Layer(HDSkins.getInstance().getSkinPrioritySorter().createDynamicTextures(layers))
        ))::get;
    }

    public static Optional<PlayerSkins> of(Avatar player) {
        return ClientPlayerInfo.of(player).map(ClientPlayerInfo::getSkins);
    }
}
