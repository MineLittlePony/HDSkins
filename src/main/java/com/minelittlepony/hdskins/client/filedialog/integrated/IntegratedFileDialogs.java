package com.minelittlepony.hdskins.client.filedialog.integrated;

import com.minelittlepony.hdskins.client.filedialog.FileDialog;
import com.minelittlepony.hdskins.client.filedialog.FileDialogs;

public final class IntegratedFileDialogs implements FileDialogs {
    @Override
    public FileDialog open(String title) {
        return new FileSelectorScreen(title);
    }

    @Override
    public FileDialog save(String title, String filename) {
        return new FileSaverScreen(title, filename);
    }
}
