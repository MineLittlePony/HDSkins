package com.minelittlepony.hdskins.client;

import java.nio.file.Path;

import com.minelittlepony.common.util.settings.*;
import com.minelittlepony.hdskins.client.filedialog.FileSystemUtil;

public class HDConfig extends Config {

    public final Setting<Path> lastChosenFile = value("lastChosenFile", FileSystemUtil.getUserContentDirectory(FileSystemUtil.CONTENT_TYPE_DOWNLOAD));
    public final Setting<Boolean> useNativeFileChooser = value("useNativeFileChooser", false);

    public HDConfig(Path path) {
        super(FLATTENED_JSON_ADAPTER, path);
    }
}
