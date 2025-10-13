package com.minelittlepony.hdskins.mixin.client;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.minelittlepony.hdskins.client.PlayerSkins;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.entity.player.SkinTextures;

@Mixin(PlayerSkinProvider.class)
abstract class MixinPlayerSkinProvider {
    @ModifyReturnValue(method = "supplySkinTextures", at = @At("RETURN"))
    private Supplier<SkinTextures> injectSkinTextures(Supplier<SkinTextures> supplier, GameProfile profile, boolean requireSecure) {
        return PlayerSkins.create(profile, supplier).getSkins();
    }
}
