package com.minelittlepony.hdskins.client.filedialog;

import com.minelittlepony.hdskins.client.gui.FileSaverScreen;
import com.minelittlepony.hdskins.client.gui.FileSelectorScreen;

class GuiFileDialogs implements FileDialogs {
    @Override
    public FileDialog open(String title) {
        return new FileSelectorScreen(title);
    }

    @Override
    public FileDialog save(String title, String filename) {
        return new FileSaverScreen(title, filename);
    }
}
