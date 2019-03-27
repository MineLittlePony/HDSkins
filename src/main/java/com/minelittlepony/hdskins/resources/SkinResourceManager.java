package com.minelittlepony.hdskins.resources;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SkinResourceManager implements IResourceManagerReloadListener {

    private static final Logger logger = LogManager.getLogger();

    private static final Gson gson = new Gson();

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private Map<UUID, Skin> uuidSkins = Maps.newHashMap();
    private Map<String, Skin> namedSkins = Maps.newHashMap();

    private Map<ResourceLocation, Future<ResourceLocation>> inProgress = Maps.newHashMap();
    private Map<ResourceLocation, ResourceLocation> converted = Maps.newHashMap();

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {

        uuidSkins.clear();
        namedSkins.clear();

        executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();

        inProgress.clear();
        converted.clear();

        for (String domain : resourceManager.getResourceNamespaces()) {
            try {
                resourceManager.getAllResources(new ResourceLocation(domain, "textures/skins/skins.json")).forEach(this::loadResource);
            } catch (IOException ignored) { }
        }
    }

    private void loadResource(IResource resource) {
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
    private SkinData getSkinData(IResource resource) throws JsonParseException {

        try (InputStream stream = resource.getInputStream()) {
            return gson.fromJson(new InputStreamReader(stream), SkinData.class);
        } catch (JsonParseException e) {
            logger.warn("Invalid skins.json in " + resource.getPackName(), e);
        } catch (IOException ignored) {}

        return null;
    }

    @Nullable
    public ResourceLocation getPlayerTexture(GameProfile profile, Type type) {
        if (type != Type.SKIN) {
            return null; // not supported
        }

        Skin skin = getSkin(profile);
        if (skin != null) {
            final ResourceLocation res = skin.getTexture();
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
    @Nullable
    public ResourceLocation getConvertedResource(@Nullable ResourceLocation res) {
        loadSkinResource(res);

        return converted.get(res);
    }

    private void loadSkinResource(@Nullable final ResourceLocation res) {
        if (res != null) { // read and convert in a new thread
            inProgress.computeIfAbsent(res, this::computeNewSkinResource);
        }
    }

    private CompletableFuture<ResourceLocation> computeNewSkinResource(ResourceLocation res) {
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
