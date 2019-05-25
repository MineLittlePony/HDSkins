package com.minelittlepony.hdskins.resources.texture;

import com.minelittlepony.hdskins.HDSkins;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.texture.NativeImage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ImageBufferDownloadHD implements ISkinAvailableCallback {

    @Nonnull
    private ISkinAvailableCallback callback = ISkinAvailableCallback.NULL;

    private Type skinType = Type.SKIN;

    public ImageBufferDownloadHD() {

    }

    public ImageBufferDownloadHD(Type type, @Nonnull ISkinAvailableCallback callback) {
        this.callback = callback;
        this.skinType = type;
    }

    @Override
    @Nullable
    public NativeImage parseUserSkin(@Nullable NativeImage downloadedImage) {

        // TODO: Do we want to convert other skin types?
        if (downloadedImage == null || skinType != Type.SKIN) {
            return downloadedImage;
        }

        int imageWidth = downloadedImage.getWidth();
        int imageHeight = downloadedImage.getHeight();

        if (imageHeight == imageWidth) {
            return downloadedImage;
        }

        int scale = imageWidth / 64;

        NativeImage image = new NativeImage(imageWidth, imageWidth, true);
        image.copyFrom(downloadedImage);

        downloadedImage.close();
        
        SimpleDrawer drawer = () -> image;

        // copy layers
        // leg
        drawer.draw(scale, 4, 16, 16, 32, 4, 4, true, false); // top
        drawer.draw(scale, 8, 16, 16, 32, 4, 4, true, false); // bottom
        drawer.draw(scale, 0, 20, 24, 32, 4, 12, true, false); // inside
        drawer.draw(scale, 4, 20, 16, 32, 4, 12, true, false); // front
        drawer.draw(scale, 8, 20, 8, 32, 4, 12, true, false); // outside
        drawer.draw(scale, 12, 20, 16, 32, 4, 12, true, false); // back
        // arm
        drawer.draw(scale, 44, 16, -8, 32, 4, 4, true, false); // top
        drawer.draw(scale, 48, 16, -8, 32, 4, 4, true, false); // bottom
        drawer.draw(scale, 40, 20, 0, 32, 4, 12, true, false);// inside
        drawer.draw(scale, 44, 20, -8, 32, 4, 12, true, false);// front
        drawer.draw(scale, 48, 20, -16, 32, 4, 12, true, false);// outside
        drawer.draw(scale, 52, 20, -8, 32, 4, 12, true, false); // back

        // mod things
        HDSkins.getInstance().convertSkin(drawer);

        return callback.parseUserSkin(image);
    }

    @Override
    public void skinAvailable() {
        callback.skinAvailable();
    }
}
