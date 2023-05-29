package com.minelittlepony.hdskins.client.filedialog;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

class NativeFileDialogs implements FileDialogs {
    @Override
    public FileDialog open(String title) {
        return new AbstractNativeFileDialog() {
            @Override
            @Nullable
            protected String runFileDialog() {
                return TinyFileDialogs.tinyfd_openFileDialog(title, this.currentDirectory.toString(), getFilterBuffer(this.extensionFilter), this.filterMessage, false);
            }
        };
    }

    @Override
    public FileDialog save(String title, String filename) {
        return new AbstractNativeFileDialog() {
            @Override
            @Nullable
            protected String runFileDialog() {
                if (Files.isDirectory(this.currentDirectory)) {
                    this.currentDirectory = this.currentDirectory.resolve(filename);
                } else {
                    this.currentDirectory = this.currentDirectory.resolveSibling(filename);
                }

                return TinyFileDialogs.tinyfd_saveFileDialog(title, this.currentDirectory.toString(), getFilterBuffer(this.extensionFilter), this.filterMessage);
            }
        };
    }

    private static PointerBuffer getFilterBuffer(@Nullable String filter) {
        if (filter == null) {
            return null;
        }
        return PointerBuffer.create(ByteBuffer.wrap(filter.getBytes(StandardCharsets.UTF_8)));

    }
}