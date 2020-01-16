package com.minelittlepony.hdskins.client.resources;

import net.minecraft.client.texture.NativeImage;

@FunctionalInterface
public interface ImageFilter {
    NativeImage filterImage(NativeImage image);
}
