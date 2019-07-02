package com.minelittlepony.hdskins.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloadListener;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

/**
 * TODO: What do we even need this for?
 */
@Deprecated
public class SkinResourceManager implements ResourceReloadListener {

    private static final Logger logger = LogManager.getLogger();

    private static final Gson gson = new Gson();

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private Map<UUID, Skin> uuidSkins = Maps.newHashMap();
    private Map<String, Skin> namedSkins = Maps.newHashMap();

    private Map<Identifier, Future<Identifier>> inProgress = Maps.newHashMap();
    private Map<Identifier, Identifier> converted = Maps.newHashMap();

    @Override
    public CompletableFuture<Void> reload(Synchronizer sync, ResourceManager sender,
            Profiler serverProfiler, Profiler clientProfiler,
            Executor serverExecutor, Executor clientExecutor) {

        sync.getClass();
        return sync.whenPrepared(null).thenRunAsync(() -> {
            clientProfiler.startTick();
            clientProfiler.push("Reloading User's HD Skins");
            reloadSkins(sender);
            clientProfiler.pop();
            clientProfiler.endTick();
        }, clientExecutor);
    }

    private void reloadSkins(ResourceManager resourceManager) {

        uuidSkins.clear();
        namedSkins.clear();

        executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();

        inProgress.clear();
        converted.clear();

        for (String domain : resourceManager.getAllNamespaces()) {
            try {
                resourceManager.getAllResources(new Identifier(domain, "textures/skins/skins.json")).forEach(this::loadResource);
            } catch (IOException ignored) { }
        }
    }

    private void loadResource(Resource resource) {
        SkinData data = getSkinData(resource);

        if (data == null) {
            return;
        }

        for (Skin s : data.skins) {

            if (s.uuid != null) {
                uuidSkins.put(s.uuid, s);
            }

            if (s.name != null) {
                namedSkins.put(s.name, s);
            }
        }
    }

    @Nullable
    private SkinData getSkinData(Resource resource) throws JsonParseException {

        try (InputStream stream = resource.getInputStream()) {
            return gson.fromJson(new InputStreamReader(stream), SkinData.class);
        } catch (JsonParseException e) {
            logger.warn("Invalid skins.json in " + resource.getResourcePackName(), e);
        } catch (IOException ignored) {}

        return null;
    }

    @Deprecated
    @Nullable
    public Identifier getPlayerTexture(GameProfile profile, Type type) {
        if (type != Type.SKIN) {
            return null; // not supported
        }

        Skin skin = getSkin(profile);
        if (skin != null) {
            final Identifier res = skin.getTexture();
            return getConvertedResource(res);
        }
        return null;
    }

    /**
     * Convert older resources to a newer format.
     *
     * @param res The skin resource to convert
     * @return The converted resource
     */
    @Deprecated
    @Nullable
    public Identifier getConvertedResource(@Nullable Identifier res) {
        loadSkinResource(res);

        return converted.get(res);
    }

    private void loadSkinResource(@Nullable final Identifier res) {
        if (res != null) { // read and convert in a new thread
            inProgress.computeIfAbsent(res, this::computeNewSkinResource);
        }
    }

    private CompletableFuture<Identifier> computeNewSkinResource(Identifier res) {
        return CompletableFuture.supplyAsync(new ImageLoader(res), executor).whenComplete((loc, t) -> {
            if (loc != null) {
                converted.put(res, loc);
            } else {
                LogManager.getLogger().warn("Errored while processing {}. Using original.", res, t);
                converted.put(res, res);
            }
        });
    }

    @Nullable
    private Skin getSkin(GameProfile profile) {
        Skin skin = uuidSkins.get(profile.getId());

        if (skin == null) {
            return namedSkins.get(profile.getName());
        }

        return skin;
    }
}
