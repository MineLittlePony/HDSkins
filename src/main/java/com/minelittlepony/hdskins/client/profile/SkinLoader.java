package com.minelittlepony.hdskins.client.profile;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.cache.LoadingCache;
import com.minelittlepony.hdskins.HDSkinsServer;
import com.minelittlepony.hdskins.Memoize;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.PlayerSkinLayers;
import com.minelittlepony.hdskins.client.SkinCacheClearCallback;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public class SkinLoader {
    private final LoadingCache<GameProfile, CompletableFuture<ProvidedSkins>> cache = Memoize.createAsyncLoadingCache(15, profile -> {
        if (HDSkins.getInstance().getConfig().useBatchLoading.get()) {
            return HDSkinsServer.getInstance().fillProfile(profile).thenComposeAsync(this::fetchTextures, Minecraft.getInstance());
        }

        return CompletableFuture.supplyAsync(() -> HDSkinsServer.getInstance().getServers().fillProfile(profile), Util.nonCriticalIoPool())
                .thenComposeAsync(this::fetchTextures, Minecraft.getInstance());
    });

    private final FileStore fileStore = new FileStore();

    public DynamicSkinTextures get(GameProfile profile) {
        return new DynamicSkinTextures() {
            private long cacheTime = System.currentTimeMillis();
            private ProvidedSkins value = ProvidedSkins.EMPTY;

            {
                load(profile).thenAccept(result -> {
                    value = result;
                    cacheTime = System.currentTimeMillis();
                });
            }

            @Override
            public Set<Identifier> getProvidedSkinTypes() {
                return value.getProvidedSkinTypes();
            }

            @Override
            public Optional<ClientAsset.Texture> getSkin(SkinType type) {
                return value.getSkin(type);
            }

            @Override
            public Optional<String> getModel() {
                return value.getModel();
            }

            @Override
            public boolean isNewer(long age) {
                return cacheTime > age;
            }
        };
    }

    public CompletableFuture<ProvidedSkins> load(GameProfile profile) {
        return cache.getUnchecked(profile);
    }

    private CompletableFuture<ProvidedSkins> fetchTextures(Map<SkinType, MinecraftProfileTexture> textures) {
        Map<SkinType, CompletableFuture<ClientAsset.Texture>> tasks = textures.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> fileStore.get(entry.getKey(), entry.getValue()).thenApply(id -> new ClientAsset.ResourceTexture(id, id))
        ));

        return CompletableFuture.allOf(tasks.values().stream().toArray(CompletableFuture[]::new)).thenApply(_ -> {
            return new ProvidedSkins(
                    Optional.ofNullable(textures.get(SkinType.SKIN)).map(skin -> VanillaModels.of(skin.getMetadata("model"))),
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
        HDSkinsServer.getInstance().getServers().invalidateProfiles();
        PlayerSkinLayers.invalidateCaches();

        fileStore.clear();
        SkinCacheClearCallback.EVENT.invoker().onSkinCacheCleared();
    }

    public record ProvidedSkins (Optional<String> model, Set<Identifier> providedSkinTypes, Map<SkinType, ClientAsset.Texture> skins) implements DynamicSkinTextures {
        public static final ProvidedSkins EMPTY = new ProvidedSkins(Optional.empty(), Set.of(), Map.of());

        @Override
        public Set<Identifier> getProvidedSkinTypes() {
            return providedSkinTypes;
        }

        @Override
        public Optional<ClientAsset.Texture> getSkin(SkinType type) {
            return Optional.ofNullable(skins.get(type));
        }

        @Override
        public Optional<String> getModel() {
            return model;
        }

        @Override
        public boolean isNewer(long age) {
            return false;
        }
    }
}
