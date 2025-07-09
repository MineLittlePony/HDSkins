package com.minelittlepony.hdskins.client.gui;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

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
}
