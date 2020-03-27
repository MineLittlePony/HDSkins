package com.minelittlepony.hdskins.client.resources;

import static com.minelittlepony.common.event.SkinFilterCallback.EVENT;
import static com.minelittlepony.common.event.SkinFilterCallback.copy;

import java.io.File;

import javax.annotation.Nullable;

import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.util.Identifier;

public class HDPlayerSkinTexture extends PlayerSkinTexture implements ImageFilter {

    private final SkinType skinType;

    public HDPlayerSkinTexture(File cacheFile, String url, SkinType skinType, Identifier fallbackSkin, Runnable runnable) {
        super(cacheFile, url, fallbackSkin, false, runnable);
        this.skinType = skinType;
    }

    @Nullable
    @Override
    public NativeImage filterImage(@Nullable NativeImage image) {
        // TODO: Do we want to convert other skin types?
        if (skinType != SkinType.SKIN) {
            return image;
        }
        return filterPlayerSkins(image);
    }

    @Nullable
    public static NativeImage filterPlayerSkins(@Nullable NativeImage image) {

        if (image == null) {
            return image;
        }

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        boolean legacy = imageWidth != imageHeight;

        if (imageHeight != imageWidth) {

            NativeImage image2 = new NativeImage(imageWidth, imageWidth, true);
            image2.copyFrom(image);
            image.close();
            image = image2;

            // copy layers
            // leg
            copy(image, 4, 16, 16, 32, 4, 4, true, false); // top
            copy(image, 8, 16, 16, 32, 4, 4, true, false); // bottom
            copy(image, 0, 20, 24, 32, 4, 12, true, false); // inside
            copy(image, 4, 20, 16, 32, 4, 12, true, false); // front
            copy(image, 8, 20, 8, 32, 4, 12, true, false); // outside
            copy(image, 12, 20, 16, 32, 4, 12, true, false); // back
            // arm
            copy(image, 44, 16, -8, 32, 4, 4, true, false); // top
            copy(image, 48, 16, -8, 32, 4, 4, true, false); // bottom
            copy(image, 40, 20, 0, 32, 4, 12, true, false);// inside
            copy(image, 44, 20, -8, 32, 4, 12, true, false);// front
            copy(image, 48, 20, -16, 32, 4, 12, true, false);// outside
            copy(image, 52, 20, -8, 32, 4, 12, true, false); // back

        }
        // mod things
        EVENT.invoker().processImage(image, legacy);

        return image;
    }

}
