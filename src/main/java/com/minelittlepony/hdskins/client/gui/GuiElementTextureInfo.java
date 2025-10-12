package com.minelittlepony.hdskins.client.gui;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;

public record GuiElementTextureInfo(
        long time,
        @Nullable
        GpuTexture texture,
        @Nullable
        GpuTextureView textureView,
        @Nullable
        GpuTexture depthTexture,
        @Nullable
        GpuTextureView depthTextureView) implements AutoCloseable {

    public boolean isClosed() {
        return texture == null || texture.isClosed();
    }

    @Override
    public void close() {
        if (texture != null && !texture.isClosed()) {
            texture.close();
        }

        if (textureView != null && !textureView.isClosed()) {
            textureView.close();
        }

        if (depthTexture != null && !depthTexture.isClosed()) {
            depthTexture.close();
        }

        if (depthTextureView != null && !depthTextureView.isClosed()) {
            depthTextureView.close();
        }
    }

    public static final class StateTracker<T extends SpecialGuiElementRenderState> implements AutoCloseable {
        private int textureStateHash;
        private final Int2ObjectMap<GuiElementTextureInfo> textures = new Int2ObjectOpenHashMap<>();
        @Nullable
        private GuiElementTextureInfo state;

        @Nullable
        public GuiElementTextureInfo getRestoredState() {
            return state;
        }

        public boolean update(@Nullable GpuTexture texture, @Nullable GpuTextureView textureView, @Nullable GpuTexture depthTexture, @Nullable GpuTextureView depthTextureView, T elementState) {
            state = null;
            final int newHash = elementState.hashCode();
            if (newHash == textureStateHash) {
                return false;
            }

            long now = System.currentTimeMillis();
            if (texture != null && textureStateHash != 0) {
                textures.put(textureStateHash, new GuiElementTextureInfo(now, texture, textureView, depthTexture, depthTextureView));
            }
            textureStateHash = newHash;
            textures.int2ObjectEntrySet().removeIf(entry -> {
                if (entry.getValue().time() < now - 100) {
                    entry.getValue().close();
                    return true;
                }
                return entry.getValue().isClosed();
            });
            state = textures.get(newHash);
            return true;
        }

        @Override
        public void close() {
            textures.values().forEach(GuiElementTextureInfo::close);
            textures.clear();
        }
    }

}
