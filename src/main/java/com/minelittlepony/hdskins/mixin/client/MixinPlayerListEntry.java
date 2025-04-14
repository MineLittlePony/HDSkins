package com.minelittlepony.hdskins.mixin.client;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minelittlepony.hdskins.client.PlayerSkins;
import com.minelittlepony.hdskins.client.ducks.ClientPlayerInfo;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;

@Mixin(PlayerListEntry.class)
abstract class MixinPlayerListEntry implements ClientPlayerInfo {
    @Shadow @Mutable
    private @Final Supplier<SkinTextures> texturesSupplier;

    private Supplier<PlayerSkins> hdskinsPlayerSkins;

    @Inject(method = "<init>(Lcom/mojang/authlib/GameProfile;Z)V", at = @At("RETURN"))
    private void onInit(GameProfile profile, boolean secureChatEnforced, CallbackInfo info) {
        hdskinsPlayerSkins = PlayerSkins.create(profile, texturesSupplier);
        texturesSupplier = () -> getSkins().sorted().getSkinTextures();
    }

    @Override
    public PlayerSkins getSkins() {
        return hdskinsPlayerSkins.get();
    }
}
