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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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

    private int textureStateHash;
    private final Int2ObjectMap<GuiElementTextureInfo> textures = new Int2ObjectOpenHashMap<>();

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(T elementState, GuiRenderState state, int windowScaleFactor, CallbackInfo cbi) {
        int newHash = elementState.hashCode();
        if (newHash != textureStateHash) {
            long now = System.currentTimeMillis();
            if (texture != null && textureStateHash != 0) {
                textures.put(textureStateHash, new GuiElementTextureInfo(now, texture, textureView, depthTexture, depthTextureView));
            }
            textures.int2ObjectEntrySet().removeIf(entry -> {
                if (entry.getValue().time() < now - 100) {
                    entry.getValue().close();
                    return true;
                }
                return false;
            });
            textureStateHash = newHash;
            texture = null;
            @Nullable
            GuiElementTextureInfo info = textures.get(newHash);
            if (info != null) {
                texture = info.texture();
                textureView = info.textureView();
                depthTexture = info.depthTexture();
                depthTextureView = info.depthTextureView();
            }
        }
    }

    @Inject(method = "close()V", at = @At("HEAD"))
    private void onClose(CallbackInfo info) {
        textures.values().forEach(GuiElementTextureInfo::close);
        textures.clear();
    }
}
