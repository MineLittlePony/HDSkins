package com.minelittlepony.hdskins.client.filedialog;

import com.minelittlepony.hdskins.client.filedialog.integrated.IntegratedFileDialogs;
import com.minelittlepony.hdskins.client.filedialog.os.NativeFileDialogs;

public interface FileDialogs {
    FileDialogs INTEGRATED = new IntegratedFileDialogs();
    FileDialogs NATIVE = new NativeFileDialogs();

    FileDialogs DEFAULT = FileSystemUtil.IS_SANDBOXED ? NATIVE : INTEGRATED;

    FileDialog open(String title);

    FileDialog save(String title, String filename);
}
