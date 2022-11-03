package com.minelittlepony.hdskins.client;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.minelittlepony.common.util.settings.*;

public class HDConfig extends Config {

    public final Setting<Path> lastChosenFile = value("lastChosenFile", Paths.get("/"));

    public HDConfig(Path path) {
        super(FLATTENED_JSON_ADAPTER, path);
    }
}
