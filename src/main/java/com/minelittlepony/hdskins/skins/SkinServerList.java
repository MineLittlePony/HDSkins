package com.minelittlepony.hdskins.skins;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.minelittlepony.hdskins.skins.api.SkinServer;
import com.minelittlepony.common.util.GamePaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkinServerList {

    private static final String SKIN_SERVERS = "skin-servers.json";

    private static final Logger logger = LogManager.getLogger();
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(SkinServer.class, SkinServerSerializer.instance)
            .create();

    @Nullable
    private SkinServerJson skinServers;

    public void load() {
        logger.info("Loading skin servers");

        Path jsonPath = GamePaths.getConfigDirectory().resolve(SKIN_SERVERS);

        skinServers = null;

        try {
            if (Files.notExists(jsonPath)) {
                logger.info("Skin server config not found. Saving defaults.");
                try (InputStream in = getClass().getResourceAsStream("/skin-servers.json")) {
                    Files.copy(in, jsonPath);
                }
            }
            try (Reader r = Files.newBufferedReader(jsonPath)) {
                skinServers = gson.fromJson(r, SkinServerJson.class);
            }
        } catch (IOException e) {
            logger.warn("Unable to load skin servers. No servers will be used.", e);
        }

        if (skinServers != null) {
            skinServers.servers.add(YggdrasilSkinServer.INSTANCE);
        }
    }

    public List<SkinServer> getSkinServers() {
        return skinServers == null ? Collections.emptyList() : ImmutableList.copyOf(skinServers.servers);
    }

    public Set<String> getWhitelist() {
        return skinServers == null ? Collections.emptySet() : ImmutableSet.copyOf(skinServers.whitelist);
    }

    public Set<String> getBlacklist() {
        return skinServers == null ? Collections.emptySet() : skinServers.blacklist;
    }

    private static class SkinServerJson {
        List<SkinServer> servers = new ArrayList<>();
        Set<String> whitelist = new HashSet<>();
        Set<String> blacklist = new HashSet<>();
    }

}
