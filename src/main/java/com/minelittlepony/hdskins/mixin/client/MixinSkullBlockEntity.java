package com.minelittlepony.hdskins.mixin.client;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jetbrains.annotations.Nullable;

@Mixin(SkullBlockEntity.class)
abstract class MixinSkullBlockEntity {
    @Inject(method = "hasTextures(Lcom/mojang/authlib/GameProfile;)Z", at = @At(value = "HEAD"), cancellable = true)
    private static void onHasTextures(@Nullable GameProfile profile, CallbackInfoReturnable<Boolean> info) {
        if (profile != null && profile.getProperties().containsKey("hd_textures")) {
            info.setReturnValue(true);
        }
    }
}
