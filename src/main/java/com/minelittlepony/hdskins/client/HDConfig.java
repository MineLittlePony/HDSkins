package com.minelittlepony.hdskins.client;

import com.minelittlepony.common.util.settings.JsonConfig;
import com.minelittlepony.common.util.settings.Setting;

import java.nio.file.Path;
import java.nio.file.Paths;

public class HDConfig extends JsonConfig {

    public final Setting<Path> lastChosenFile = value("lastChosenFile", Paths.get("/"));

    public HDConfig(Path path) {
        super(path);
    }
}
