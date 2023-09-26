package com.minelittlepony.hdskins.client;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.minelittlepony.hdskins.client.ducks.ClientPlayerInfo;
import com.minelittlepony.hdskins.client.profile.DynamicSkinTextures;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public record PlayerSkins (
        Layer vanilla,
        Layer hd,
        Layer combined
) {
    public static Optional<PlayerSkins> of(AbstractClientPlayerEntity player) {
        return ClientPlayerInfo.of(player).map(ClientPlayerInfo::getSkins);
    }

    public static PlayerSkins of(GameProfile profile, Supplier<SkinTextures> vanillaSkins) {
        var vanilla = new Layer(DynamicSkinTextures.of(vanillaSkins), vanillaSkins);
        var hd = new Layer(Suppliers.memoize(() -> DynamicSkinTextures.union(
                HDSkins.getInstance().getResourceManager().getSkinTextures(profile),
                HDSkins.getInstance().getProfileRepository().get(profile)
        ))::get);
        var combined = new Layer(Suppliers.memoize(() -> DynamicSkinTextures.union(hd.dynamic(), vanilla.dynamic()))::get);
        return new PlayerSkins(vanilla, hd, combined);
    }

    public record Layer (Supplier<DynamicSkinTextures> dynamic, Supplier<SkinTextures> resolved) {
        public Layer(Supplier<DynamicSkinTextures> dynamic) {
            this(dynamic, Suppliers.memoizeWithExpiration(() -> dynamic.get().toSkinTextures(), 1, TimeUnit.SECONDS)::get);
        }

        public Set<Identifier> getProvidedSkinTypes() {
            return dynamic().get().getProvidedSkinTypes();
        }

        public Optional<Identifier> getSkin(SkinType type) {
            return dynamic().get().getSkin(type);
        }

        public String getModel() {
            return Objects.requireNonNullElse(dynamic().get().getModel(VanillaModels.DEFAULT), VanillaModels.DEFAULT);
        }

        public SkinTextures getSkinTextures() {
            return resolved().get();
        }
    }
}
