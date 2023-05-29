package com.minelittlepony.hdskins.client.filedialog;

import com.minelittlepony.hdskins.client.HDSkins;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

abstract class AbstractNativeFileDialog implements FileDialog {
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    protected Path currentDirectory = HDSkins.getInstance().getConfig().lastChosenFile.get();

    @Nullable
    protected String extensionFilter;

    @Nullable
    protected String filterMessage;

    private Callback callback = (file, done) -> {};

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
    protected abstract String runFileDialog();

    @Override
    public FileDialog launch() {
        CompletableFuture.supplyAsync(() -> {
            var file = runFileDialog();
            if (file == null) {
                return null;
            }
            return Paths.get(file);
        }, EXECUTOR).thenAcceptAsync(result -> {
            callback.onDialogClosed(result, true);
        }, MinecraftClient.getInstance());
        return this;
    }
}
