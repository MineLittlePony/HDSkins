package com.minelittlepony.hdskins.client.gui;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.gui.filesystem.FileDialogs;
import com.minelittlepony.hdskins.client.gui.filesystem.FileSystemUtil;
import com.minelittlepony.hdskins.client.gui.filesystem.WatchedFile;
import com.minelittlepony.hdskins.client.gui.player.skins.ServerPlayerSkins.RemoteTexture;
import com.minelittlepony.hdskins.client.resources.HDPlayerSkinTextureDownloader;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

    public static final Component ERR_UNREADABLE = Component.translatable("hdskins.error.unreadable");
    public static final Component ERR_EXT = Component.translatable("hdskins.error.ext");
    public static final Component ERR_OPEN = Component.translatable("hdskins.error.open");
    public static final Component ERR_INVALID_TOO_LARGE = Component.translatable("hdskins.error.invalid.too_large");
    public static final Component ERR_INVALID_SHAPE = Component.translatable("hdskins.error.invalid.shape");
    public static final Component ERR_INVALID_POWER_OF_TWO = Component.translatable("hdskins.error.invalid.power_of_two");
    public static final Component ERR_INVALID = Component.translatable("hdskins.error.invalid");

    public static final Component MSG_CHOOSE = Component.translatable("hdskins.choose");

    private boolean pickingInProgress;
    private final DualCarouselWidget<?> previewer;
    private Consumer<SkinType> listener = _ -> {};

    private final List<Function<NativeImage, Component>> validators = new ArrayList<>();

    private final WatchedFile localSkin = new WatchedFile(this::fileChanged, this::fileRemoved);

    private volatile Component status = MSG_CHOOSE;

    public SkinChooser(DualCarouselWidget<?> previewer) {
        this.previewer = previewer;
        addImageValidation(this::acceptsSkinDimensions);
    }

    public void addSkinChangedEventListener(Consumer<SkinType> listener) {
        this.listener = this.listener.andThen(listener);
    }

    private FileDialogs getFileDialogs() {
        if ((FileSystemUtil.IS_SANDBOXED && HDSkins.getInstance().getConfig().enableSandboxingCheck.get())
            || HDSkins.getInstance().getConfig().useNativeFileChooser.get()) {
            return FileDialogs.NATIVE;
        }

        return FileDialogs.INTEGRATED;
    }

    public void addImageValidation(Function<NativeImage, Component> validator) {
        validators.add(validator);
    }

    private void fileRemoved() {
        Minecraft.getInstance().execute(previewer.getLocal().getSkins()::close);
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

    public Component getStatus() {
        return status;
    }

    @Override
    public List<Component> getStatusLines() {
        return List.of(getStatus());
    }

    public int getStatusColor(Component status) {
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
                RemoteTexture texture = previewer.getRemote().getSkins().get(previewer.getActiveSkinType());
                if (texture.isReady() && texture.texture() != null) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException ignored) { }

                    try (InputStream response = texture.texture().openStream()) {
                        Files.copy(response, file);

                        Minecraft.getInstance().gui.setScreen(new ConfirmationScreen(Minecraft.getInstance().gui.screen(), Component.translatable("hdskins.save.completed"), () -> {
                            Util.getPlatform().openPath(file);
                        }));
                    } catch (IOException e) {
                        LogManager.getLogger().error("Failed to save remote skin.", e);
                    }
                }
            }
        }).launch();
    }

    public void selectFile(Path skinFile) {
        status = evaluateAndSelect(skinFile);
    }

    private Component evaluateAndSelect(Path skinFile) {
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
    protected Component acceptsSkinDimensions(NativeImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        if (previewer.getActiveSkinType().isVanilla()) {
            if (!HDPlayerSkinTextureDownloader.isPowerOfTwo(w)) {
                return ERR_INVALID_POWER_OF_TWO;
            }
            if (!HDPlayerSkinTextureDownloader.isValidShape(w, h)) {
                return ERR_INVALID_SHAPE;
            }
        }

        if (w > MAX_SKIN_DIMENSION || h > MAX_SKIN_DIMENSION) {
            return ERR_INVALID_TOO_LARGE;
        }

        return null;
    }

    public interface EventListener {
        default void onSetLocalSkin(SkinType type) {}
    }
}
