package com.minelittlepony.hdskins.mixin.client;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minelittlepony.hdskins.client.gui.GuiElementTextureInfo;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;

@Mixin(SpecialGuiElementRenderer.class)
public abstract class MixinSpecialGuiElementRenderer<T extends SpecialGuiElementRenderState> implements AutoCloseable {
    @Shadow
    @Nullable
    private GpuTexture texture;
    @Shadow
    @Nullable
    private GpuTextureView textureView;
    @Shadow
    @Nullable
    private GpuTexture depthTexture;
    @Shadow
    @Nullable
    private GpuTextureView depthTextureView;

    private final GuiElementTextureInfo.StateTracker<T> stateTracker = new GuiElementTextureInfo.StateTracker<>();

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(T elementState, GuiRenderState state, int windowScaleFactor, CallbackInfo cbi) {
        if (stateTracker.update(texture, textureView, depthTexture, depthTextureView, elementState)) {
            texture = null;
        }
        @Nullable
        GuiElementTextureInfo info = stateTracker.getRestoredState();
        if (info != null) {
            texture = info.texture();
            textureView = info.textureView();
            depthTexture = info.depthTexture();
            depthTextureView = info.depthTextureView();
        }
    }

    @Inject(method = "close()V", at = @At("HEAD"))
    private void onClose(CallbackInfo info) {
        stateTracker.close();
    }
}
