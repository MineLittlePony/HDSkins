package com.minelittlepony.hdskins.mixin;

import net.minecraft.client.renderer.texture.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.NativeImage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ThreadDownloadImageData.class)
public interface MixinThreadDownloadImageData extends IBufferedTexture {

    @Accessor("bufferedImage")
    @Override
    NativeImage getBufferedImage();
}
