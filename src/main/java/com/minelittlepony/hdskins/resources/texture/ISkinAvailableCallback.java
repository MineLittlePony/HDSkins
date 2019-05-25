package com.minelittlepony.hdskins.resources.texture;

import net.minecraft.client.texture.ImageFilter;
import net.minecraft.client.texture.NativeImage;

@FunctionalInterface
public interface ISkinAvailableCallback extends ImageFilter {
    
    ISkinAvailableCallback NULL = () -> {};
    
    @Override
    default NativeImage filterImage(NativeImage image) {
        return parseUserSkin(image);
    }
    
    @Override
    default void method_3238() {
        skinAvailable();
    }

    default NativeImage parseUserSkin(NativeImage image) {
        return image;
    }

    void skinAvailable();
}