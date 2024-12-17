package com.minelittlepony.hdskins.client.gui.filesystem.os;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.minelittlepony.hdskins.client.gui.filesystem.FileDialog;
import com.minelittlepony.hdskins.client.gui.filesystem.FileDialogs;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class NativeFileDialogs implements FileDialogs {
    @Override
    public FileDialog open(String title) {
        return new AbstractNativeFileDialog() {
            @Override
            @Nullable
            protected String runFileDialog() {
                return TinyFileDialogs.tinyfd_openFileDialog(
                        sanitize(title),
                        currentDirectory.toString(),
                        getFilterBuffer(extensionFilter),
                        sanitize(filterMessage), false);
            }
        };
    }

    @Override
    public FileDialog save(String title, String filename) {
        return new AbstractNativeFileDialog() {
            @Override
            @Nullable
            protected String runFileDialog() {
                currentDirectory = Files.isDirectory(currentDirectory) ? currentDirectory.resolve(filename) : currentDirectory.resolveSibling(filename);
                return TinyFileDialogs.tinyfd_saveFileDialog(
                        sanitize(title), currentDirectory.toString(),
                        getFilterBuffer(extensionFilter),
                        sanitize(filterMessage)
                );
            }
        };
    }

    private static String sanitize(String input) {
        return input.replaceAll("[\"\']", "").replaceAll("([|&\\[\\]$()`\\\\])", "\\\\$1");
    }

    private static PointerBuffer getFilterBuffer(@Nullable String filter) {
        if (filter == null) {
            return null;
        }
        return PointerBuffer.create(ByteBuffer.wrap(filter.getBytes(StandardCharsets.UTF_8)));
    }
}
