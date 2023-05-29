package com.minelittlepony.hdskins.client.filedialog;

public interface FileDialogs {
    FileDialogs INTEGRATED = new GuiFileDialogs();
    FileDialogs NATIVE = new NativeFileDialogs();

    FileDialog open(String title);

    FileDialog save(String title, String filename);
}
