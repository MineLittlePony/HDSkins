package com.minelittlepony.hdskins.client;

import com.minelittlepony.hdskins.client.file.FileDialogs;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SkinChooser {

    public static final int MAX_SKIN_DIMENSION = 1024;

    public static final String[] EXTENSIONS = new String[]{"png", "PNG"};

    public static final Text ERR_UNREADABLE = new TranslatableText("hdskins.error.unreadable");
    public static final Text ERR_EXT = new TranslatableText("hdskins.error.ext");
    public static final Text ERR_OPEN = new TranslatableText("hdskins.error.open");
    public static final Text ERR_INVALID_TOO_LARGE = new TranslatableText("hdskins.error.invalid.too_large");
    public static final Text ERR_INVALID_SHAPE = new TranslatableText("hdskins.error.invalid.shape");
    public static final Text ERR_INVALID_POWER_OF_TWO = new TranslatableText("hdskins.error.invalid.power_of_two");
    public static final Text ERR_INVALID = new TranslatableText("hdskins.error.invalid");

    public static final Text MSG_CHOOSE = new TranslatableText("hdskins.choose");

    private static boolean isPowerOfTwo(int number) {
        return number != 0 && (number & number - 1) == 0;
    }

    private boolean pickingInProgress;

    private final SkinUploader uploader;

    private volatile Text status = MSG_CHOOSE;

    public SkinChooser(SkinUploader uploader) {
        this.uploader = uploader;
    }

    public boolean pickingInProgress() {
        return pickingInProgress;
    }

    public Text getStatus() {
        return status;
    }

    public void openBrowsePNG(String title) {
        pickingInProgress = true;
        FileDialogs.open(title)
                .addExtensionFilter(".png")
                .setDescription("PNG Files (*.png)")
                .launch()
                .thenAccept(file -> {
                    pickingInProgress = false;

                    if (file != null) {
                        selectFile(file);
                    }
                });
    }

    public void openSavePNG(String title, String filename) {
        pickingInProgress = true;
        FileDialogs.save(title, filename)
                .addExtensionFilter(".png")
                .setDescription("PNG Files (*.png")
                .launch()
                .thenAccept(file -> {
                    pickingInProgress = false;
                    if (file != null) {
                        try (InputStream response = uploader.downloadSkin()) {
                            Files.copy(response, file);
                        } catch (IOException e) {
                            LogManager.getLogger().error("Failed to save remote skin.", e);
                        }
                    }
                });
    }

    public void selectFile(Path skinFile) {
        status = evaluateAndSelect(skinFile);
    }

    private Text evaluateAndSelect(Path skinFile) {
        if (!Files.exists(skinFile)) {
            return ERR_UNREADABLE;
        }

        if (!FilenameUtils.isExtension(skinFile.getFileName().toString(), EXTENSIONS)) {
            return ERR_EXT;
        }

        try (InputStream in = Files.newInputStream(skinFile)) {
            NativeImage chosenImage = NativeImage.read(in);

            Text err = acceptsSkinDimensions(chosenImage.getWidth(), chosenImage.getHeight());
            if (err != null) {
                return err;
            }

            uploader.setLocalSkin(skinFile);

            return MSG_CHOOSE;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ERR_OPEN;
    }

    @Nullable
    protected Text acceptsSkinDimensions(int w, int h) {
        if (!isPowerOfTwo(w)) {
            return ERR_INVALID_POWER_OF_TWO;
        }
        if (w > MAX_SKIN_DIMENSION) {
            return ERR_INVALID_TOO_LARGE;
        }
        if (!(w == h || w == h * 2)) {
            return ERR_INVALID_SHAPE;
        }
        return null;
    }
}
