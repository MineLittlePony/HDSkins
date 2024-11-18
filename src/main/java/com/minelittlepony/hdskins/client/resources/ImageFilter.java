package com.minelittlepony.hdskins.client.resources;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.texture.NativeImage;

@FunctionalInterface
public interface ImageFilter {
    @Nullable NativeImage filterImage(@Nullable NativeImage image);
}
