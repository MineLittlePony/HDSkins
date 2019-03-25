package com.minelittlepony.hdskins.upload;

import java.io.File;

@Deprecated // no more swing
@FunctionalInterface
public interface IFileCallback {
    void onDialogClosed(File file, int dialogResults);
}
