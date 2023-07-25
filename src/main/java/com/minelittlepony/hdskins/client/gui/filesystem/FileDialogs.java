package com.minelittlepony.hdskins.client.gui.filesystem;

import com.minelittlepony.hdskins.client.gui.filesystem.integrated.IntegratedFileDialogs;
import com.minelittlepony.hdskins.client.gui.filesystem.os.NativeFileDialogs;

public interface FileDialogs {
    FileDialogs INTEGRATED = new IntegratedFileDialogs();
    FileDialogs NATIVE = new NativeFileDialogs();

    FileDialog open(String title);

    FileDialog save(String title, String filename);
}
