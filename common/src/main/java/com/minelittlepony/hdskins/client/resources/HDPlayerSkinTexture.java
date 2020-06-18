package com.minelittlepony.hdskins.client.resources;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

import static com.minelittlepony.common.event.SkinFilterCallback.copy;

public class HDPlayerSkinTexture extends PlayerSkinTexture {

    private static final Logger logger = LogManager.getLogger();

    public HDPlayerSkinTexture(PlayerSkinTexture texture) {
        super(texture.cacheFile, texture.url, texture.location, texture.convertLegacy, texture.loadedCallback);
    }

    @Nullable
    @Override
    protected NativeImage loadTexture(InputStream stream) {
        NativeImage nativeImage = null;

        try {
            nativeImage = NativeImage.read(stream);
            if (this.convertLegacy) {
                nativeImage = filterPlayerSkins(nativeImage);
            }
        } catch (IOException var4) {
            logger.warn("Error while loading the skin texture", var4);
        }

        return nativeImage;
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
        // TODO do this differently
        // EVENT.invoker().processImage(image, legacy);

        return image;
    }

}
