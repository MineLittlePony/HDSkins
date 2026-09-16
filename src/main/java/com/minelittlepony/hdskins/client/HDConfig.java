package com.minelittlepony.hdskins.client;

import java.nio.file.Path;

import com.minelittlepony.common.client.gui.VisibilityMode;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.common.util.settings.*;
import com.minelittlepony.hdskins.client.gui.ButtonLocation;
import com.minelittlepony.hdskins.client.gui.filesystem.FileSystemUtil;

public class HDConfig extends Config {

    private static HDConfig config;

    public static HDConfig getInstance() {
        if (config == null) {
            config = new HDConfig(GamePaths.getConfigDirectory().resolve("hdskins.json"));
            config.load();
        }
        return config;
    }

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
    public final Setting<ButtonLocation> pantsButtonLocation = value("gui", "mainMenuButtonLocation", ButtonLocation.ICON)
            .addComment("Where to put the hd skins uploader button on the main menu")
            .addComment("ICON (default) - next to the accessibility button")
            .addComment("TOP_LEFT")
            .addComment("TOP_RIGHT")
            .addComment("BOTTOM_LEFT")
            .addComment("BOTTOM_RIGHT");

    public final Setting<Integer> skinBatching = value("networking", "skinBatching", 2)
            .addComment("Default: 2 ticks")
            .addComment("Max: 200 ticks")
            .addComment("Controls the number of ticks to wait when batching requests together to load player skins.")
            .addComment("Higher values will batch more skins together but it may take longer overall for player's skins to be loaded.")
            .addComment("Lower values are faster but will cause more server load.")
            .addComment("Setting it to 0 or below will disable batching entirely (legacy behaviour).");

    public HDConfig(Path path) {
        super(HEIRARCHICAL_JSON_ADAPTER, path);
    }
}
