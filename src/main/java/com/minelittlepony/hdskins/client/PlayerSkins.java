package com.minelittlepony.hdskins.client;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Suppliers;
import com.minelittlepony.hdskins.client.ducks.ClientPlayerInfo;
import com.minelittlepony.hdskins.client.profile.DynamicSkinTextures;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public class PlayerSkins {
    @Nullable
    public static PlayerSkins of(AbstractClientPlayerEntity player) {
        ClientPlayerInfo info = ClientPlayerInfo.of(player);
        if (info == null) {
            return null;
        }
        return info.getSkins();
    }

    private final Supplier<DynamicSkinTextures> skins;
    private final Supplier<SkinTextures> combinedSkinTextures;

    public PlayerSkins(GameProfile profile, Supplier<SkinTextures> vanillaSkins) {
        this.skins = Suppliers.memoize(() -> {
            return DynamicSkinTextures.union(
                    HDSkins.getInstance().getResourceManager().getSkinTextures(profile),
                    DynamicSkinTextures.union(
                            HDSkins.getInstance().getProfileRepository().get(profile),
                            DynamicSkinTextures.of(vanillaSkins)
                    )
            );
        })::get;
        this.combinedSkinTextures = Suppliers.memoizeWithExpiration(() -> skins.get().toSkinTextures(), 1, TimeUnit.SECONDS)::get;
    }

    public Set<Identifier> getProvidedSkinTypes() {
        return skins.get().getProvidedSkinTypes();
    }

    @Nullable
    public Identifier getSkin(SkinType type) {
        return skins.get().getSkin(type).orElse(null);
    }

    @Nullable
    public String getModel() {
        return skins.get().getModel(VanillaModels.DEFAULT);
    }

    public SkinTextures getSkinTextures() {
        return combinedSkinTextures.get();
    }
}
