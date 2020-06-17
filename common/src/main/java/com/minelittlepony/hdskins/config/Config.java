package com.minelittlepony.hdskins.config;

import com.minelittlepony.common.util.settings.ToStringAdapter;
import com.minelittlepony.hdskins.client.HDSkins;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {

    public static ConfigFile<Config> FILE = new ConfigFile.Builder<Config>()
            .withPath(Paths.get(HDSkins.MOD_ID, "config.json"))
            .withType(Config.class)
            .withVersion(1)
            .withGson(builder -> builder.registerTypeAdapter(Path.class, new ToStringAdapter<>(Paths::get)))
            .withDefault(() -> new Config("."))
            .build();

    public Path lastChosenFile;

    private Config(String lastChosenFile) {
        this.lastChosenFile = Paths.get(lastChosenFile);
    }
}
