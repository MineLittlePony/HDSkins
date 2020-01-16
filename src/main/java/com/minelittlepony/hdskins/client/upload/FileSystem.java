package com.minelittlepony.hdskins.client.upload;

import com.minelittlepony.hdskins.client.gui.GuiFileSaver;
import com.minelittlepony.hdskins.client.gui.GuiFileSelector;

/**
 * Access point for launching file open/save dialogues.
 */
public class FileSystem {
    /**
     * Creates a new dialogue for selecting and reading a file.
     */
    public static IFileDialog openRead(String windowTitle) {
        return new GuiFileSelector(windowTitle);
    }

    /**
     * Creates a new dialogue for selecting a location to save a file.
     */
    public static IFileDialog openWrite(String windowTitle, String filename) {
        return new GuiFileSaver(windowTitle, filename);
    }
}
