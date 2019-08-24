package com.minelittlepony.hdskins.resources.texture;

import javax.annotation.Nullable;

import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.texture.ImageFilter;
import net.minecraft.client.texture.NativeImage;

import static com.minelittlepony.common.event.SkinFilterCallback.*;

public class ImageBufferDownloadHD implements ImageFilter {

    private final Runnable callback;

    private final SkinType skinType;

    public ImageBufferDownloadHD() {
        this(SkinType.SKIN, () -> {});
    }

    public ImageBufferDownloadHD(SkinType type, Runnable callback) {
        this.callback = callback;
        this.skinType = type;
    }

    @Override
    @Nullable
    public NativeImage filterImage(@Nullable NativeImage image) {

        // TODO: Do we want to convert other skin types?
        if (image == null || skinType != SkinType.SKIN) {
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

    @Override
    public void method_3238() {
        callback.run();
    }
}
