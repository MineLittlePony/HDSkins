package com.minelittlepony.hdskins.client.resources;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

@FunctionalInterface
public interface ImageFilter {
    @Nullable NativeImage filterImage(@Nullable NativeImage image);
}
