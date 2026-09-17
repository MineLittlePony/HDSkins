package com.minelittlepony.hdskins.client.gui.filesystem.os;

import org.jetbrains.annotations.Nullable;
import com.minelittlepony.hdskins.client.gui.filesystem.FileDialog;
import com.minelittlepony.hdskins.client.gui.filesystem.FileDialogs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class NativeFileDialogs implements FileDialogs {
    @Override
    public FileDialog open(String title) {
        return new AbstractNativeFileDialog() {
            @Override
            @Nullable
            protected CompletableFuture<@Nullable Path> runFileDialog() {
                return Blaze3DDialog.openFile(currentDirectory,
                        extensionFilter == null
                            ? null
                            : List.of(new Blaze3DDialog.DialogFileFilter(filterMessage, extensionFilter)),
                            false
                        )
                        .thenApply(files -> files.isEmpty() ? null : files.getFirst());
            }
        };
    }

    @Override
    public FileDialog save(String title, String filename) {
        return new AbstractNativeFileDialog() {
            @Override
            @Nullable
            protected CompletableFuture<@Nullable Path> runFileDialog() {
                currentDirectory = Files.isDirectory(currentDirectory) ? currentDirectory.resolve(filename) : currentDirectory.resolveSibling(filename);
                return Blaze3DDialog.saveFile(currentDirectory,
                        extensionFilter == null
                            ? null
                            : List.of(new Blaze3DDialog.DialogFileFilter(filterMessage, extensionFilter))
                        )
                        .thenApply(files -> files.isEmpty() ? null : files.getFirst());
            }
        };
    }
}
