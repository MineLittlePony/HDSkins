package com.minelittlepony.hdskins.client.file;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * A file dialog for opening and reading files.
 * 
 * Implementations may vary.
 */
public interface FileDialog {

    /**
     * Sets the description of the filter.
     */
    FileDialog setDescription(String description);

    /**
     * Adds an extension filter to the dialog.
     */
    FileDialog addExtensionFilter(String extension);

    /**
     * Launches the dialogue.
     */
    CompletableFuture<Path> launch();
}
