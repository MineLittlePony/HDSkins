package com.minelittlepony.hdskins.client.resources;

import static com.minelittlepony.common.event.SkinFilterCallback.EVENT;
import static com.minelittlepony.common.event.SkinFilterCallback.copy;
import static com.minelittlepony.common.event.SkinFilterCallback.fill;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.minelittlepony.common.event.SkinFilterCallback;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.util.net.URIUtil;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;


public class HDPlayerSkinTextureDownloader {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static CompletableFuture<Identifier> downloadAndRegisterTexture(Identifier textureId, Path cacheFile, String uri, SkinType skinType) {
        return CompletableFuture.supplyAsync(() -> {
            NativeImage nativeImage;
            try {
                nativeImage = download(cacheFile, uri);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return SkinType.SKIN.equals(skinType) ? remapTexture(nativeImage) : nativeImage;
        }, Util.nonCriticalIoPool().forName("downloadTexture")).thenCompose(image -> TextureLoader.uploadTexture(textureId, image));
    }

    private static NativeImage download(Path path, String uri) throws IOException {
        if (Files.isRegularFile(path)) {
            LOGGER.debug("Loading HTTP texture from local cache ({})", path);
            try (InputStream stream = Files.newInputStream(path)) {
                return NativeImage.read(stream);
            }
        }

        LOGGER.debug("Downloading HTTP texture from {} to {}", uri, path);
        byte[] response = URIUtil.getBytes(URI.create(uri));
        try {
            FileUtil.createDirectoriesSafe(path.getParent());
            Files.write(path, response);
        } catch (IOException e) {
            LOGGER.warn("Failed to cache texture {} in {}", uri, path);
        }

        return NativeImage.read(response);
    }

    @Nullable
    public static NativeImage remapTexture(@Nullable NativeImage image) {

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
                int k = image.getPixel(i, j);
                if ((k >> 24 & 0xFF) >= 128) continue;
                return;
            }
        }
        for (i = x1; i < x2; ++i) {
            for (j = y1; j < y2; ++j) {
                image.setPixel(i, j, image.getPixel(i, j) & 0xFFFFFF);
            }
        }
    }

    private static void stripAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int i = x1; i < x2; ++i) {
            for (int j = y1; j < y2; ++j) {
                image.setPixel(i, j, ARGB.opaque(image.getPixel(i, j)));
            }
        }
    }
}
