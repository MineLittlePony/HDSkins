package com.minelittlepony.hdskins.config;

import com.google.common.collect.ImmutableList;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.skins.SkinServer;
import com.minelittlepony.hdskins.skins.SkinServerSerializer;
import com.minelittlepony.hdskins.skins.types.ValhallaSkinServer;
import com.minelittlepony.hdskins.skins.types.YggdrasilSkinServer;

import java.nio.file.Paths;
import java.util.List;

public class SkinConfig {

    public static final ConfigFile<SkinConfig> FILE = new ConfigFile.Builder<SkinConfig>()
            .withPath(Paths.get(HDSkins.MOD_ID, "skinservers.json"))
            .withType(SkinConfig.class)
            .withVersion(1)
            .withGson(gson -> gson.registerTypeAdapter(SkinServer.class, new SkinServerSerializer()))
            .withDefault(() -> new SkinConfig(ImmutableList.of(
                    new ValhallaSkinServer("https://skins.minelittlepony-mod.com"),
                    new YggdrasilSkinServer()
            )))
            .build();

    public List<SkinServer> servers;

    private SkinConfig(List<SkinServer> servers) {
        this.servers = servers;
    }

}
