package com.minelittlepony.hdskins.client.gui.filesystem.os;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.gui.filesystem.FileDialog;

import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

abstract class AbstractNativeFileDialog implements FileDialog {
    protected Path currentDirectory = HDSkins.getInstance().getConfig().lastChosenFile.get();

    @Nullable
    protected String extensionFilter;

    @Nullable
    protected String filterMessage;

    private Callback callback = (_, _) -> {};

    @Override
    public FileDialog startIn(Path currentDirectory) {
        this.currentDirectory = currentDirectory;
        return this;
    }

    @Override
    public FileDialog filter(String extension, String description) {
        this.extensionFilter = extension;
        this.filterMessage = description;
        return this;
    }

    @Override
    public FileDialog andThen(Callback callback) {
        this.callback = callback;
        return this;
    }

    @Nullable
    protected abstract CompletableFuture<@Nullable Path> runFileDialog();

    @Override
    public FileDialog launch() {
        runFileDialog().thenAcceptAsync(result -> callback.onDialogClosed(result, true), Minecraft.getInstance());
        return this;
    }
}
