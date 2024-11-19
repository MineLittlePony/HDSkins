package com.minelittlepony.hdskins.client.resources;

import static com.minelittlepony.common.event.SkinFilterCallback.EVENT;
import static com.minelittlepony.common.event.SkinFilterCallback.copy;
import static com.minelittlepony.common.event.SkinFilterCallback.fill;

import java.io.File;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.common.event.SkinFilterCallback;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

public class HDPlayerSkinTexture extends PlayerSkinTexture implements ImageFilter {

    private final SkinType skinType;

    public HDPlayerSkinTexture(File cacheFile, String url, SkinType skinType, Identifier id, Runnable runnable) {
        super(cacheFile, url, id, false, runnable);
        this.skinType = skinType;
    }

    @Nullable
    @Override
    public NativeImage filterImage(@Nullable NativeImage image) {
        if (image == null || !SkinType.SKIN.equals(skinType)) {
            return image;
        }
        return filterPlayerSkins(image, TextureLoader.Exclusion.NULL);
    }

    @Nullable
    public static NativeImage filterPlayerSkins(@Nullable NativeImage image, TextureLoader.Exclusion exclusion) {

        if (image == null) {
            return image;
        }

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        if (!isPowerOfTwo(imageWidth) || !isValidShape(imageWidth, imageHeight)) {
            image.close();
            HDSkins.LOGGER.warn("Discarding incorrectly sized ({}x{}) skin texture", imageWidth, imageHeight);
            return null;
        }

        if (imageHeight != imageWidth) {
            NativeImage image2 = new NativeImage(imageWidth, imageWidth, true);
            image2.copyFrom(image);
            image.close();
            image = image2;

            fill(image, 0, 32, 64, 32, 0);
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

        if (!EVENT.invoker().shouldAllowTransparency(image, imageWidth, imageHeight)) {
            int scale = SkinFilterCallback.getResolutionScale(imageWidth, imageHeight);
            stripAlpha(image, 0, 0, 32 * scale, 16 * scale);
            if (SkinFilterCallback.isLegacyAspectRatio(imageWidth, imageHeight)) {
                stripColor(image, 32 * scale, 0, 64 * scale, 32 * scale);
            }
            stripAlpha(image, 0, 16 * scale, 64 * scale, 32 * scale);
            stripAlpha(image, 16 * scale, 48 * scale, 48 * scale, 64 * scale);
        }
        // mod things
        return EVENT.invoker().processImage(image, imageWidth, imageHeight);
    }

    public static boolean isPowerOfTwo(int number) {
        return number != 0 && (number & number - 1) == 0;
    }

    public static boolean isValidShape(int w, int h) {
        return (w > 0 && h > 0) && ((w == h) || (w == (2 * h)));
    }

    private static void stripColor(NativeImage image, int x1, int y1, int x2, int y2) {
        int j;
        int i;
        for (i = x1; i < x2; ++i) {
            for (j = y1; j < y2; ++j) {
                int k = image.getColorArgb(i, j);
                if ((k >> 24 & 0xFF) >= 128) continue;
                return;
            }
        }
        for (i = x1; i < x2; ++i) {
            for (j = y1; j < y2; ++j) {
                image.setColorArgb(i, j, image.getColorArgb(i, j) & 0xFFFFFF);
            }
        }
    }

    private static void stripAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int i = x1; i < x2; ++i) {
            for (int j = y1; j < y2; ++j) {
                image.setColorArgb(i, j, ColorHelper.fullAlpha(image.getColorArgb(i, j)));
            }
        }
    }
}
