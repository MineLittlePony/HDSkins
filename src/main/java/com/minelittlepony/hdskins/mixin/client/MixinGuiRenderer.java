package com.minelittlepony.hdskins.mixin.client;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.minelittlepony.hdskins.client.gui.RenderStateKeys;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.CubeMapTexture;
import net.minecraft.resources.Identifier;

@Mixin(GuiRenderer.class)
abstract class MixinGuiRenderer {
    @Unique
    private final Map<Identifier, CubeMap> cubemaps = new HashMap<>();

    @Shadow
    private @Final GuiRenderState renderState;

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "net/minecraft/client/gui/render/GuiRenderer.cubeMap:Lnet/minecraft/client/renderer/CubeMap;"))
    private CubeMap redirectCubeMap(CubeMap original) {
        if (renderState.panoramaRenderState != null) {
            Identifier texture = renderState.panoramaRenderState.getData(RenderStateKeys.PANORAMA_TEXTURE_KEY);
            if (texture != null) {
                return cubemaps.computeIfAbsent(texture, t -> {
                    CubeMap map = new CubeMap(t);
                    Minecraft.getInstance().getTextureManager().registerAndLoad(t, new CubeMapTexture(t));
                    return map;
                });
            }
        }
        return original;
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void onClose(CallbackInfo info) {
        cubemaps.values().forEach(i -> i.close());
    }
}
