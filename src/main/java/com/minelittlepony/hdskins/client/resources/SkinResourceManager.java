package com.minelittlepony.hdskins.client.resources;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.minelittlepony.hdskins.Memoize;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.profile.DynamicSkinTextures;
import com.minelittlepony.hdskins.client.resources.SkinResourceManager.SkinData.Skin;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

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

    private static final Identifier ID = HDSkins.id("skins");

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(SkinType.class, SkinType.adapter())
            .create();

    private final TextureLoader loader = new TextureLoader("hd_skins", (image, exclusions) -> HDPlayerSkinTextureDownloader.remapTexture(image));

    private final Map<SkinType, SkinStore> store = new HashMap<>();
    private long lastLoadTime;

    private final LoadingCache<Identifier, CompletableFuture<Identifier>> textures = Memoize.createAsyncLoadingCache(15, loader::loadAsync);

    @Override
    public CompletableFuture<Void> reload(Synchronizer sync, ResourceManager sender, Executor serverExecutor, Executor clientExecutor) {
        return sync.whenPrepared(null).thenRunAsync(() -> {
            store.clear();
            loader.stop();

            textures.invalidateAll();

            sender.getAllNamespaces().stream().map(domain -> Identifier.of(domain, "textures/skins/skins.json")).forEach(identifier -> {
                sender.getAllResources(identifier).stream()
                    .map(this::loadSkinData)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(data -> {
                        data.skins.forEach(s -> {
                            store.computeIfAbsent(s.getType(), SkinStore::new).addSkin(s);
                        });
                    });
            });

            lastLoadTime = System.currentTimeMillis();
        }, clientExecutor);
    }

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    private Optional<SkinData> loadSkinData(Resource res) {
        try (var reader = new InputStreamReader(res.getInputStream())) {
            return Optional.ofNullable(GSON.fromJson(reader, SkinData.class));
        } catch (JsonParseException e) {
            LOGGER.warn("Invalid skins.json in " + res.getPackId(), e);
        } catch (IOException ignored) {}

        return Optional.empty();
    }

    public DynamicSkinTextures getSkinTextures(GameProfile profile) {
        return new DynamicSkinTextures() {
            private long initTime = System.currentTimeMillis();
            @Override
            public Set<Identifier> getProvidedSkinTypes() {
                return Set.of();
            }

            @Override
            public Optional<Identifier> getSkin(SkinType type) {
                return getCustomPlayerTexture(profile, type);
            }

            @Override
            public Optional<String> getModel() {
                return getCustomPlayerModel(profile);
            }

            @Override
            public boolean hasChanged() {
                if (initTime < lastLoadTime) {
                    initTime = System.currentTimeMillis();
                    return true;
                }
                return false;
            }
        };
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
                        return HDSkins.id(skin);
                    }

                    return Identifier.of(skin);
                }

                return HDSkins.id(String.format("textures/skins/%s.png", skin));
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
