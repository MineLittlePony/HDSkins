package com.minelittlepony.hdskins.client.gui;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.gui.filesystem.FileDialogs;
import com.minelittlepony.hdskins.client.gui.filesystem.FileSystemUtil;
import com.minelittlepony.hdskins.client.gui.filesystem.WatchedFile;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

/**
 * Provides functionality for loading and saving skin files into the previewer,
 * as well as input validation.
 */
public class SkinChooser implements CarouselStatusLabel {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final int MAX_SKIN_DIMENSION = 1024;

    public static final String[] EXTENSIONS = new String[]{"png", "PNG"};

    public static final Text ERR_UNREADABLE = Text.translatable("hdskins.error.unreadable");
    public static final Text ERR_EXT = Text.translatable("hdskins.error.ext");
    public static final Text ERR_OPEN = Text.translatable("hdskins.error.open");
    public static final Text ERR_INVALID_TOO_LARGE = Text.translatable("hdskins.error.invalid.too_large");
    public static final Text ERR_INVALID_SHAPE = Text.translatable("hdskins.error.invalid.shape");
    public static final Text ERR_INVALID_POWER_OF_TWO = Text.translatable("hdskins.error.invalid.power_of_two");
    public static final Text ERR_INVALID = Text.translatable("hdskins.error.invalid");

    public static final Text MSG_CHOOSE = Text.translatable("hdskins.choose");

    private static boolean isPowerOfTwo(int number) {
        return number != 0 && (number & number - 1) == 0;
    }

    private boolean pickingInProgress;
    private final DualCarouselWidget previewer;
    private Consumer<SkinType> listener = t -> {};

    private final List<Function<NativeImage, Text>> validators = new ArrayList<>();

    private final WatchedFile localSkin = new WatchedFile(this::fileChanged, this::fileRemoved);

    private volatile Text status = MSG_CHOOSE;

    public SkinChooser(DualCarouselWidget previewer) {
        this.previewer = previewer;
        addImageValidation(this::acceptsSkinDimensions);
    }

    public void addSkinChangedEventListener(Consumer<SkinType> listener) {
        this.listener = this.listener.andThen(listener);
    }

    private FileDialogs getFileDialogs() {
        if ((FileSystemUtil.IS_SANDBOXED && !HDSkins.getInstance().getConfig().enableSandboxingCheck.get())
            || HDSkins.getInstance().getConfig().useNativeFileChooser.get()) {
            return FileDialogs.NATIVE;
        }

        return FileDialogs.INTEGRATED;
    }

    public void addImageValidation(Function<NativeImage, Text> validator) {
        validators.add(validator);
    }

    private void fileRemoved() {
        MinecraftClient.getInstance().execute(previewer.getLocal().getSkins()::close);
    }

    private void fileChanged(Path path) {
        try {
            SkinType skinType = previewer.getActiveSkinType();
            LOGGER.debug("Set {} {}", skinType, path);
            previewer.getLocal().getSkins().get(skinType).setLocal(path);
            listener.accept(skinType);
        } catch (IOException e) {
            HDSkins.LOGGER.error("Could not load local path `" + path + "`", e);
        }
    }

    public boolean pickingInProgress() {
        return pickingInProgress;
    }

    public Text getStatus() {
        return status;
    }

    @Override
    public List<Text> getStatusLines() {
        return List.of(getStatus());
    }

    public int getStatusColor(Text status) {
        return status == MSG_CHOOSE ? WHITE : RED;
    }

    @Override
    public boolean hasStatus() {
        return getStatus() != MSG_CHOOSE || !hasSelection();
    }

    public boolean hasSelection() {
        return !localSkin.isPending() && localSkin.isSet();
    }

    @Nullable
    public URI getSelection() {
        return localSkin.toUri();
    }

    public void update() {
        localSkin.update();
    }

    public void openBrowsePNG(String title) {
        pickingInProgress = true;
        getFileDialogs().open(title)
                .filter(".png", "PNG Files (*.png)")
                .andThen((file, success) -> {
            pickingInProgress = false;

            if (success) {
                selectFile(file);
            }
        }).launch();
    }

    public void openSavePNG(SkinUploader uploader, String title, String filename) {
        getFileDialogs().save(title, filename)
                .filter(".png", "PNG Files (*.png)")
                .andThen((file, success) -> {
            pickingInProgress = false;

            if (success) {
                previewer.getRemote().getSkins().get(previewer.getActiveSkinType()).texture().ifPresent(texture -> {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException ignored) { }

                    try (InputStream response = texture.openStream()) {
                        Files.copy(response, file);

                        MinecraftClient.getInstance().setScreen(new ConfirmationScreen(MinecraftClient.getInstance().currentScreen, Text.translatable("hdskins.save.completed"), () -> {
                            Util.getOperatingSystem().open(file.toUri());
                        }));
                    } catch (IOException e) {
                        LogManager.getLogger().error("Failed to save remote skin.", e);
                    }
                });
            }
        }).launch();
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

            return validators.stream()
                .map(f -> f.apply(chosenImage))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> {
                    localSkin.set(skinFile);

                    return MSG_CHOOSE;
                });
        } catch (IOException e) {
            HDSkins.LOGGER.error("Exception occured whilst loading image file {}.", skinFile, e);
        }

        return ERR_OPEN;
    }

    @Nullable
    protected Text acceptsSkinDimensions(NativeImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        if (previewer.getActiveSkinType().isVanilla()) {
            if (!isPowerOfTwo(w)) {
                return ERR_INVALID_POWER_OF_TWO;
            }
            if (!(w == h || w == h * 2)) {
                return ERR_INVALID_SHAPE;
            }
        }

        if (w > MAX_SKIN_DIMENSION) {
            return ERR_INVALID_TOO_LARGE;
        }

        return null;
    }

    public interface EventListener {
        default void onSetLocalSkin(SkinType type) {}
    }
}
