package com.minelittlepony.hdskins.resources;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.minelittlepony.hdskins.resources.SkinResourceManager.SkinData.Skin;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

/**
 * A resource manager for players to specify their own skin overrides.
 *
 * hdskins:textures/skins/skins.json
 * {
 *      "skins": [
 *          { "type": "SKIN", "model": "default", "name": "Sollace", "skin": "hdskins:textures/skins/super_silly_pony.png" }
 *      ]
 * }
 *
 */
public class SkinResourceManager implements IdentifiableResourceReloadListener {

    private static final Identifier ID = new Identifier("hdskins", "skins");

    private static final Logger logger = LogManager.getLogger();

    private static final Gson gson = new Gson();

    private final ImageLoader loader = new ImageLoader();

    private final Map<Type, SkinStore> store = new EnumMap<>(Type.class);

    private final Map<Identifier, Identifier> textures = Maps.newHashMap();

    @Override
    public CompletableFuture<Void> reload(Synchronizer sync, ResourceManager sender,
            Profiler serverProfiler, Profiler clientProfiler,
            Executor serverExecutor, Executor clientExecutor) {

        sync.getClass();
        return sync.whenPrepared(null).thenRunAsync(() -> {
            clientProfiler.startTick();
            clientProfiler.push("Reloading User's HD Skins");

            store.clear();
            loader.stop();

            textures.clear();

            sender.getAllNamespaces().stream().map(domain -> new Identifier(domain, "textures/skins/skins.json")).forEach(identifier -> {
                try {
                    sender.getAllResources(identifier).stream()
                        .map(this::loadSkinData)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .forEach(data -> {
                            data.skins.forEach(s -> {
                                store.computeIfAbsent(s.getType(), SkinStore::new).addSkin(s);
                            });
                        });
                } catch (IOException ignored) { }
            });

            clientProfiler.pop();
            clientProfiler.endTick();
        }, clientExecutor);
    }

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Nullable
    private Optional<SkinData> loadSkinData(Resource res) throws JsonParseException {
        try (Resource resource = res) {
            return Optional.ofNullable(gson.fromJson(new InputStreamReader(resource.getInputStream()), SkinData.class));
        } catch (JsonParseException e) {
            logger.warn("Invalid skins.json in " + res.getResourcePackName(), e);
        } catch (IOException ignored) {}

        return Optional.empty();
    }

    /**
     * Gets a custom texture for the given profile as defined in the current resourcepack(s).
     */
    public Optional<Identifier> getCustomPlayerTexture(GameProfile profile, Type type) {
        return store.computeIfAbsent(type, SkinStore::new).getSkin(profile)
                .map(Skin::getTexture)
                .map(id -> convertTexture(type, id));
    }

    /**
     * Gets a custom model type for the given profile as defined in the current resourcepacks(s).
     */
    public Optional<String> getCustomPlayerModel(GameProfile profile) {
        return store.computeIfAbsent(Type.SKIN, SkinStore::new).getSkin(profile)
            .map(Skin::getModel);
    }

    /**
     * Pushes the given texture through the skin parsing + conversion pipeline.
     *
     * Returns the passed identifier, otherwise the new identifier following conversion.
     */
    public Identifier convertTexture(Type type, Identifier identifier) {
        if (type != Type.SKIN) {
            return identifier;
        }

        return textures.computeIfAbsent(identifier, id -> {
            loader.loadAsync(id).whenComplete((loc, throwable) -> {
                if (throwable != null) {
                    LogManager.getLogger().warn("Errored while processing {}. Using original.", identifier, throwable);
                }

                textures.put(identifier, loc);
            });

            return id;
        });
    }

    static class SkinStore {
        private final Map<UUID, Skin> uuids = Maps.newHashMap();
        private final Map<String, Skin> names = Maps.newHashMap();

        SkinStore(Type type) { }

        public void addSkin(Skin skin) {
            if (skin.skin != null) {
                if (skin.uuid != null) {
                    uuids.put(skin.uuid, skin);
                }

                if (skin.name != null) {
                    names.put(skin.name, skin);
                }
            }
        }

        @Nullable
        public Optional<Skin> getSkin(GameProfile profile) {
            Skin skin = uuids.get(profile.getId());

            if (skin == null) {
                return Optional.ofNullable(names.get(profile.getName()));
            }

            return Optional.ofNullable(skin);
        }
    }

    static class SkinData {
        @Expose
        List<Skin> skins;

        static class Skin {
            @Expose
            Type type;

            @Expose
            String name;

            @Expose
            UUID uuid;

            @Expose
            String skin;

            @Expose
            String model;

            @Nullable
            private Identifier texture;

            public String getModel() {
                return model;
            }

            public Identifier getTexture() {
                if (texture == null) {
                    texture = createTexture();
                }

                return texture;
            }

            private Identifier createTexture() {
                if (skin.indexOf('/') > -1 || skin.indexOf(':') > -1 || skin.indexOf('.') > -1) {
                    if (skin.indexOf('.') == -1) {
                        skin += ".png";
                    }
                    if (skin.indexOf(':') == -1) {
                        return new Identifier("hdskins", skin);
                    }

                    return new Identifier(skin);
                }

                return new Identifier("hdskins", String.format("textures/skins/%s.png", skin));
            }

            public Type getType() {
                return type == null ? Type.SKIN : type;
            }
        }
    }
}
