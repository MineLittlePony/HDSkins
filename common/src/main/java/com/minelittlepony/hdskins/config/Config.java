package com.minelittlepony.hdskins.config;

import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.util.ToStringAdapter;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {

    public static ConfigFile<Config> FILE = new ConfigFile.Builder<Config>()
            .withPath(Paths.get(HDSkins.MOD_ID, "config.json"))
            .withType(Config.class)
            .withVersion(1)
            .withGson(builder -> builder.registerTypeHierarchyAdapter(Path.class, new ToStringAdapter<>(Paths::get)))
            .withDefault(() -> new Config("."))
            .build();

    public Path lastChosenFile;

    private Config(String lastChosenFile) {
        this.lastChosenFile = Paths.get(lastChosenFile);
    }
}
