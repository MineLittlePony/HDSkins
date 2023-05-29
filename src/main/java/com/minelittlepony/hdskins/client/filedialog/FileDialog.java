package com.minelittlepony.hdskins.client.filedialog;

import java.nio.file.Path;

/**
 * A file dialog for opening and reading files.
 * <p>
 * Implementations may vary.
 */
public interface FileDialog {
    /**
     * Sets the starting directory for this dialogue when it's opened
     */
    FileDialog startIn(Path directory);

    /**
     * Called to filter the types of files this dialogue is allowed to work with.
     */
    FileDialog filter(String extension, String description);

    /**
     * Sets a callback to be executed when this dialogue closes
     */
    FileDialog andThen(Callback callback);

    /**
     * Launches the dialogue.
     */
    FileDialog launch();

    @FunctionalInterface
    interface Callback {
        void onDialogClosed(Path file, boolean success);
    }
}
