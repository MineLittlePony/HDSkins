package com.minelittlepony.hdskins.mixin.client;

import java.io.InputStream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.minelittlepony.hdskins.client.resources.ImageFilter;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;

@Mixin(PlayerSkinTexture.class)
abstract class MixinPlayerSkinTexture {
    @Inject(method ="loadTexture(Ljava/io/InputStream;)Lnet/minecraft/client/texture/NativeImage;",
            at = @At("RETURN"),
            cancellable = true)
    private void onLoad(InputStream stream, CallbackInfoReturnable<NativeImage> info) {
        if (this instanceof ImageFilter) {
            info.setReturnValue(((ImageFilter)this).filterImage(info.getReturnValue()));
        }
    }
}
