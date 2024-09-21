package com.minelittlepony.hdskins.client;

import java.nio.file.Path;

import com.minelittlepony.common.client.gui.VisibilityMode;
import com.minelittlepony.common.util.settings.*;
import com.minelittlepony.hdskins.client.gui.filesystem.FileSystemUtil;

public class HDConfig extends Config {
    public final Setting<Path> lastChosenFile = value("lastChosenFile", FileSystemUtil.getUserContentDirectory(FileSystemUtil.CONTENT_TYPE_DOWNLOAD));
    public final Setting<Boolean> useNativeFileChooser = value("filesystem", "useNativeFileChooser", false)
            .addComment("When enable, will always use the native dialogue for opening and saving files");
    public final Setting<Boolean> enableSandboxingCheck = value("filesystem", "enableSandboxingCheck", true)
            .addComment("When enabled, will use the native dialogue for opening and saving files only when running inside a sandboxed environment (flatpaks)");
    public final Setting<VisibilityMode> pantsButtonVisibility = value("gui", "mainMenuButton", VisibilityMode.ON)
            .addComment("Whether to show the hd skins uploader button on the main menu")
            .addComment("AUTO - only show when Mod Menu is not installed")
            .addComment("ON (default) - always show")
            .addComment("OFF - never show");

    public final Setting<Boolean> useBatchLoading = value("experiments", "useBatchLoading", false)
            .addComment("When enabled, player skins will be requested from the server in batches (experimental)");

    public HDConfig(Path path) {
        super(HEIRARCHICAL_JSON_ADAPTER, path);
    }
}
