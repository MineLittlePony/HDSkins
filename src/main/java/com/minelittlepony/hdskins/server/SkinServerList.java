package com.minelittlepony.hdskins.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minelittlepony.hdskins.client.HDSkins;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloadListener;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

public class SkinServerList implements SynchronousResourceReloadListener, IdentifiableResourceReloadListener {

    private static final Identifier SKIN_SERVERS = new Identifier(HDSkins.MOD_ID, "skins/servers.json");

    private static final Logger logger = LogManager.getLogger();
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(SkinServer.class, SkinServerSerializer.instance)
            .create();

    private List<SkinServer> skinServers = new LinkedList<>();

    @Override
    public Identifier getFabricId() {
        return SKIN_SERVERS;
    }

    @Override
    public void apply(ResourceManager mgr) {
        skinServers.clear();

        logger.info("Loading skin servers");
        try {
            for (Resource res : mgr.getAllResources(SKIN_SERVERS)) {
                logger.info("Found {} in {}", res.getId(), res.getResourcePackName());
                try (Resource r = res) {
                    SkinServerJson json = gson.fromJson(new InputStreamReader(r.getInputStream()), SkinServerJson.class);
                    json.apply(skinServers);
                } catch (Exception e) {
                    logger.warn("Unable to load resource '{}' from '{}'", SKIN_SERVERS, res.getResourcePackName(), e);
                }
            }
        } catch (IOException e) {
            logger.error("Unable to read {} from resource packs. No servers will be used.", SKIN_SERVERS, e);
        }
    }

    public List<SkinServer> getSkinServers() {
        return ImmutableList.copyOf(skinServers);
    }

    public Iterator<SkinServer> getCycler() {
        return Iterators.cycle(getSkinServers());
    }

    private static <T> void addAllStart(List<T> list, List<T> toAdd) {
        list.addAll(0, toAdd);
    }

    private static class SkinServerJson {
        boolean overwrite = false;
        InsertType insert = InsertType.END;
        List<SkinServer> servers = Collections.emptyList();

        private void apply(List<SkinServer> skinServers) {
            if (overwrite) {
                skinServers.clear();
            }
            logger.info("Found {} servers", servers.size());
            insert.consumer.accept(skinServers, servers);
        }
    }

    private enum InsertType {
        START(SkinServerList::addAllStart),
        END(List::addAll);

        final BiConsumer<List<SkinServer>, List<SkinServer>> consumer;

        InsertType(BiConsumer<List<SkinServer>, List<SkinServer>> consumer) {
            this.consumer = consumer;
        }

    }
}
