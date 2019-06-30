package com.minelittlepony.hdskins.resources.texture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.minelittlepony.hdskins.HDSkins;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.texture.NativeImage;

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

        NativeImage image = new NativeImage(imageWidth, imageWidth, true);
        image.copyFrom(downloadedImage);

        downloadedImage.close();

        HDDrawer drawer = () -> image;

        // copy layers
        // leg
        drawer.copy(4, 16, 16, 32, 4, 4, true, false); // top
        drawer.copy(8, 16, 16, 32, 4, 4, true, false); // bottom
        drawer.copy(0, 20, 24, 32, 4, 12, true, false); // inside
        drawer.copy(4, 20, 16, 32, 4, 12, true, false); // front
        drawer.copy(8, 20, 8, 32, 4, 12, true, false); // outside
        drawer.copy(12, 20, 16, 32, 4, 12, true, false); // back
        // arm
        drawer.copy(44, 16, -8, 32, 4, 4, true, false); // top
        drawer.copy(48, 16, -8, 32, 4, 4, true, false); // bottom
        drawer.copy(40, 20, 0, 32, 4, 12, true, false);// inside
        drawer.copy(44, 20, -8, 32, 4, 12, true, false);// front
        drawer.copy(48, 20, -16, 32, 4, 12, true, false);// outside
        drawer.copy(52, 20, -8, 32, 4, 12, true, false); // back

        // mod things
        HDSkins.getInstance().convertSkin(drawer);

        return callback.parseUserSkin(image);
    }

    @Override
    public void skinAvailable() {
        callback.skinAvailable();
    }
}
