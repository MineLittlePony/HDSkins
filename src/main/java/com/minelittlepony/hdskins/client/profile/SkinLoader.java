package com.minelittlepony.hdskins.client.profile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.SkinCacheClearCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.Feature;
import com.minelittlepony.hdskins.server.Gateway;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class SkinLoader {
    private final LoadingCache<GameProfile, CompletableFuture<ProvidedSkins>> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(CacheLoader.from(profile -> {
                if (profile.getId() == null) {
                    return CompletableFuture.completedFuture(ProvidedSkins.EMPTY);
                }

                return CompletableFuture
                        .supplyAsync(() -> loadProfileTextures(profile), Util.getMainWorkerExecutor())
                        .thenComposeAsync(this::fetchTextures, MinecraftClient.getInstance());
            }));

    private final FileStore fileStore = new FileStore();

    public Supplier<ProvidedSkins> get(GameProfile profile) {
        return () -> getNow(profile);
    }

    public ProvidedSkins getNow(GameProfile profile) {
        return load(profile).getNow(ProvidedSkins.EMPTY);
    }

    public CompletableFuture<ProvidedSkins> load(GameProfile profile) {
        return cache.getUnchecked(profile);
    }

    private Map<SkinType, MinecraftProfileTexture> loadProfileTextures(GameProfile profile) {
        return HDSkins.getInstance().getSkinServerList().getEmbeddedTextures(profile).findFirst().orElseGet(() -> {
            Set<SkinType> requestedSkinTypes = SkinType.REGISTRY.stream()
                    .filter(SkinType::isKnown)
                    .collect(Collectors.toSet());

            Map<SkinType, MinecraftProfileTexture> textureMap = new HashMap<>();

            for (Gateway gateway : HDSkins.getInstance().getSkinServerList().getSkinServers()) {
                try {
                    if (!gateway.getServer().getFeatures().contains(Feature.SYNTHETIC)) {
                        gateway.getServer().loadSkins(profile).getTextures().forEach((type, texture) -> {
                            if (requestedSkinTypes.remove(type)) {
                                textureMap.putIfAbsent(type, texture);
                            }
                        });

                        if (requestedSkinTypes.isEmpty()) {
                            break;
                        }
                    }
                } catch (IOException | AuthenticationException e) {
                    HDSkins.LOGGER.trace(e);
                }
            }

            return textureMap;
        });
    }

    private CompletableFuture<ProvidedSkins> fetchTextures(Map<SkinType, MinecraftProfileTexture> textures) {
        Map<SkinType, CompletableFuture<Identifier>> tasks = textures.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> fileStore.get(entry.getKey(), entry.getValue())
        ));

        return CompletableFuture.allOf(tasks.values().stream().toArray(CompletableFuture[]::new)).thenApply(nothing -> {
            return new ProvidedSkins(
                    Optional.ofNullable(textures.get(SkinType.SKIN)).map(skin -> skin.getMetadata("model")),
                    tasks.keySet().stream().map(SkinType::getId).collect(Collectors.toSet()),
                    tasks.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                        return entry.getValue().join();
                    }))
            );
        });
    }

    public void clear() {
        HDSkins.LOGGER.info("Clearing local player skin cache");
        cache.invalidateAll();

        fileStore.clear();
        SkinCacheClearCallback.EVENT.invoker().onSkinCacheCleared();
    }

    public record ProvidedSkins (Optional<String> model, Set<Identifier> providedSkinTypes, Map<SkinType, Identifier> skins) implements DynamicSkinTextures {
        public static final ProvidedSkins EMPTY = new ProvidedSkins(Optional.empty(), Set.of(), Map.of());

        @Override
        public Set<Identifier> getProvidedSkinTypes() {
            return providedSkinTypes;
        }

        @Override
        public Optional<Identifier> getSkin(SkinType type) {
            return Optional.ofNullable(skins.get(type));
        }

        @Override
        public String getModel(String fallback) {
            return model.orElse(fallback);
        }
    }
}
