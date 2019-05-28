package com.minelittlepony.hdskins.upload;

//TODO: No more swing
/**
 * Access point for launching file open/save dialogues.
 */
public class FileSystem {
    /**
     * Creates a new dialogue for selecting and reading a file.
     */
    public static IFileDialog openRead(String windowTitle) {
        return new FileChooser(windowTitle);
    }

    /**
     * Creates a new dialogue for selecting a location to save a file.
     */
    public static IFileDialog openWrite(String windowTitle, String filename) {
        return new FileSaver(windowTitle, filename);
    }
}
