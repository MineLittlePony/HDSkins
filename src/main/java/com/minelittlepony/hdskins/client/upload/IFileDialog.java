package com.minelittlepony.hdskins.client.upload;

import java.nio.file.Path;

/**
 * A file dialog for opening and reading files.
 * 
 * Implementations may vary.
 */
public interface IFileDialog {

    /**
     * Called to filter the types of files this dialogue is allowed to work with.
     */
    IFileDialog filter(String extension, String description);

    /**
     * Sets a callback to be executed when this dialogue closes
     */
    IFileDialog andThen(Callback callback);

    /**
     * Launches the dialogue.
     */
    IFileDialog launch();

    @FunctionalInterface
    public interface Callback {
        void onDialogClosed(Path file, boolean success);
    }
}
