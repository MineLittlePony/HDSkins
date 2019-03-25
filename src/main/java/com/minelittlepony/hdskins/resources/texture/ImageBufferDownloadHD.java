package com.minelittlepony.hdskins.resources.texture;

import com.minelittlepony.hdskins.HDSkinManager;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.renderer.texture.NativeImage;

import javax.annotation.Nullable;

public class ImageBufferDownloadHD implements ISkinAvailableCallback {

    private int scale;
    private NativeImage image;

    private ISkinAvailableCallback callback = null;

    private Type skinType = Type.SKIN;

    public ImageBufferDownloadHD() {

    }

    public ImageBufferDownloadHD(Type type, ISkinAvailableCallback callback) {
        this.callback = callback;
        this.skinType = type;
    }

    @Override
    @Nullable
    @SuppressWarnings({"SuspiciousNameCombination", "NullableProblems"})
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
        scale = imageWidth / 64;

        image = new NativeImage(imageWidth, imageWidth, true);
        image.copyImageData(downloadedImage);
        downloadedImage.close();

        // copy layers
        // leg
        drawImage(4, 16, 16, 32, 4, 4, true, false); // top
        drawImage(8, 16, 16, 32, 4, 4, true, false); // bottom
        drawImage(0, 20, 24, 32, 4, 12, true, false); // inside
        drawImage(4, 20, 16, 32, 4, 12, true, false); // front
        drawImage(8, 20, 8, 32, 4, 12, true, false); // outside
        drawImage(12, 20, 16, 32, 4, 12, true, false); // back
        // arm
        drawImage(44, 16, -8, 32, 4, 4, true, false); // top
        drawImage(48, 16, -8, 32, 4, 4, true, false); // bottom
        drawImage(40, 20, 0, 32, 4, 12, true, false);// inside
        drawImage(44, 20, -8, 32, 4, 12, true, false);// front
        drawImage(48, 20, -16, 32, 4, 12, true, false);// outside
        drawImage(52, 20, -8, 32, 4, 12, true, false); // back

        // mod things
        HDSkinManager.INSTANCE.convertSkin(image);

        if (callback != null) {
            return callback.parseUserSkin(image);
        }

        return image;
    }

    private void drawImage(int xFrom, int yFrom, int xToDelta, int yToDelta, int widthIn, int heightIn, boolean mirrorX, boolean mirrorY) {
        image.copyAreaRGBA(
                xFrom * scale, yFrom * scale, xToDelta * scale, yToDelta * scale,
                widthIn * scale, heightIn * scale, mirrorX, mirrorY);
    }

    @Override
    public void skinAvailable() {
        if (callback != null) {
            callback.skinAvailable();
        }
    }
}
