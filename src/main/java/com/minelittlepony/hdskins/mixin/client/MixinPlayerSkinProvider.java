package com.minelittlepony.hdskins.mixin.client;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.minelittlepony.hdskins.client.PlayerSkins;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.resources.SkinManager;
import net.minecraft.world.entity.player.PlayerSkin;

@Mixin(SkinManager.class)
abstract class MixinPlayerSkinProvider {
    @ModifyReturnValue(method = "createLookup", at = @At("RETURN"))
    private Supplier<PlayerSkin> injectSkinTextures(Supplier<PlayerSkin> supplier, GameProfile profile, boolean requireSecure) {
        return PlayerSkins.create(profile, supplier).getSkins();
    }
}
