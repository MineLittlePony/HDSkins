package com.minelittlepony.hdskins.client.file;

import org.lwjgl.PointerBuffer;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;

/**
 * Factory class for creating {@link FileDialog file dialogs} using tinyfd.
 */
public class FileDialogs {

    /**
     * Creates a file dialog for opening a file.
     *
     * @param title The title of the dialog window
     * @return The dialog builder
     */
    public static FileDialog open(String title) {
        return new AbstractFileDialog(title) {
            @Nullable
            @Override
            protected String runFileDialog() {
                return TinyFileDialogs.tinyfd_openFileDialog(
                        this.title,
                        this.currentDirectory.toString(),
                        getFilterBuffer(this.extensionFilters),
                        this.filterMessage,
                        false
                );
            }
        };
    }

    /**
     * Creates a file dialog for saving a file.
     *
     * @param title    The title of the dialog window
     * @param filename The desired file name of the file to save
     * @return The file dialog builder
     */
    public static FileDialog save(String title, String filename) {
        return new AbstractFileDialog(title) {
            @Nullable
            @Override
            protected String runFileDialog() {
                if (Files.isDirectory(this.currentDirectory)) {
                    this.currentDirectory = this.currentDirectory.resolve(filename);
                } else {
                    this.currentDirectory = this.currentDirectory.resolveSibling(filename);
                }

                return TinyFileDialogs.tinyfd_saveFileDialog(
                        this.title,
                        this.currentDirectory.toString(),
                        getFilterBuffer(this.extensionFilters),
                        this.filterMessage
                );
            }
        };
    }

    private static PointerBuffer getFilterBuffer(@Nullable List<String> filters) {
        if (filters == null) {
            return null;
        }
        PointerBuffer pointers = PointerBuffer.allocateDirect(filters.size());
        for (String filter : filters) {
            pointers.put(ByteBuffer.wrap(filter.getBytes()));
        }
        return pointers;
    }
}
