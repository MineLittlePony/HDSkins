package com.minelittlepony.hdskins.upload;

import java.nio.file.Path;

@FunctionalInterface
public interface IFileSystemCallback {
    void onDialogClosed(Path file, int dialogResults);
}
