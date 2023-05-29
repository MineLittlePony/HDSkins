package com.minelittlepony.hdskins.client.filedialog;

public interface FileDialogs {

    FileDialogs guiFD = new GuiFileDialogs();
    FileDialogs nativeFD = new NativeFileDialogs();

    FileDialog open(String title);

    FileDialog save(String title, String filename);
}
