package com.minelittlepony.hdskins.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.PlayerSkin;

@Mixin(CapeLayer.class)
abstract class MixinCapeLayer {
    @ModifyExpressionValue(method = "submit", at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/rendertype/RenderTypes.entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType modifyRenderType(RenderType type, @Local PlayerSkin skins) {
        return RenderTypes.entityTranslucent(skins.cape().texturePath());
    }
}
