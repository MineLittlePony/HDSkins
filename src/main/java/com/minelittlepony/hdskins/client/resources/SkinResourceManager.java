package com.minelittlepony.hdskins.client.resources;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.minelittlepony.hdskins.client.resources.SkinResourceManager.SkinData.Skin;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
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

    private static final Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(SkinType.class, SkinType.adapter())
            .create();

    private final TextureLoader loader = new TextureLoader();

    private final Map<SkinType, SkinStore> store = new HashMap<>();

    private final LoadingCache<Identifier, CompletableFuture<Identifier>> textures = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build(CacheLoader.from(loader::loadAsync));

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

            textures.invalidateAll();

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
    public Optional<Identifier> getCustomPlayerTexture(GameProfile profile, SkinType type) {
        return store.computeIfAbsent(type, SkinStore::new)
                .getSkin(profile)
                .map(Skin::getTexture)
                .map(id -> convertTexture(type, id));
    }

    /**
     * Gets a custom model type for the given profile as defined in the current resourcepacks(s).
     */
    public Optional<String> getCustomPlayerModel(GameProfile profile) {
        return store.computeIfAbsent(SkinType.SKIN, SkinStore::new)
                .getSkin(profile)
                .map(Skin::getModel);
    }

    /**
     * Pushes the given texture through the skin parsing + conversion pipeline.
     *
     * Returns the passed identifier, otherwise the new identifier following conversion.
     */
    public Identifier convertTexture(SkinType type, Identifier identifier) {
        if (type != SkinType.SKIN) {
            return identifier;
        }

        return textures.getUnchecked(identifier).getNow(identifier);
    }

    static class SkinStore {
        private final List<Skin> predicates = new ArrayList<>();

        private final Map<UUID, Skin> uuids = new HashMap<>();
        private final Map<String, Skin> names = new HashMap<>();

        SkinStore(SkinType type) { }

        public void addSkin(Skin skin) {
            if (skin.skin != null) {
                if (skin.uuid != null) {
                    uuids.put(skin.uuid, skin);
                }

                if (skin.name != null) {
                    names.put(skin.name, skin);
                }

                if (skin.getPredicate() != null) {
                    predicates.add(skin);
                }
            }
        }

        @Nullable
        public Optional<Skin> getSkin(GameProfile profile) {
            Skin skin = uuids.get(profile.getId());

            if (skin == null) {
                skin = names.get(profile.getName());

                if (skin == null) {
                    return predicates.stream().filter(f -> f.getPredicate().test(profile.getName())).findFirst();
                }
            }

            return Optional.ofNullable(skin);
        }
    }

    static class SkinData {

        List<Skin> skins;

        static class Skin {
            @Nullable
            private SkinType type;

            @Nullable
            String name;

            @Nullable
            UUID uuid;

            private String skin;

            @Nullable
            private String model;

            @Nullable
            private transient Identifier texture;

            @Nullable
            private String pattern;

            @Nullable
            private transient Predicate<String> predicate;

            public String getModel() {
                return model == null ? "default" : model;
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

            @Nullable
            public Predicate<String> getPredicate() {
                if (predicate == null && pattern != null) {
                    predicate = Pattern.compile(pattern).asPredicate();
                }

                return predicate;
            }

            public SkinType getType() {
                return type == null ? SkinType.SKIN : type;
            }
        }
    }
}
