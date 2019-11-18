package com.minelittlepony.hdskins.mixin;

import java.io.InputStream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.minelittlepony.hdskins.resources.ImageFilter;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;

@Mixin(PlayerSkinTexture.class)
public class MixinPlayerSkinTexture {
    @Inject(method ="method_22795(Lnet/minecraft/client/texture/NativeImage;)Lnet/minecraft/client/texture/NativeImage;",
            at = @At("RETURN"),
            cancellable = true)
    private void onLoad(InputStream stream, CallbackInfoReturnable<NativeImage> info) {
        if (this instanceof ImageFilter) {
            info.setReturnValue(((ImageFilter)this).filterImage(info.getReturnValue()));
        }
    }
}
