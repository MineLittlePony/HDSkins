package com.minelittlepony.hdskins;

import com.minelittlepony.common.util.settings.JsonConfig;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HDConfig extends JsonConfig {

    public final Setting<Path> lastChosenFile = new Value<>("lastChosenFile", Paths.get("/"));
}
