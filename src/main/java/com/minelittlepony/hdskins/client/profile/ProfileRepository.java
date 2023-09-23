package com.minelittlepony.hdskins.client.profile;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Suppliers;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.SkinCacheClearCallback;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.resources.HDPlayerSkinTexture;
import com.minelittlepony.hdskins.client.resources.TextureLoader;
import com.minelittlepony.hdskins.profile.SkinCallback;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ProfileRepository {
    private final ProfileCache cache;

    public ProfileRepository(HDSkins hd) {
        this.cache = new ProfileCache(hd);
    }

    public Supplier<DynamicSkinTextures> createLoader(GameProfile profile) {
        return Suppliers.memoize(() -> new DynamicSkinTextures() {
            Optional<String> model = Optional.empty();
            final Set<Identifier> providedSkinTypes = new HashSet<>();
            final Map<SkinType, Optional<Identifier>> skins = new HashMap<>();

            {
                fetchSkins(profile, (type, location, profileTexture) -> {
                    skins.put(type, Optional.of(location));
                    providedSkinTypes.add(type.getId());
                    if (type == SkinType.SKIN) {
                        model = Optional.of(VanillaModels.of(profileTexture.getMetadata("model")));
                    }
                });
            }

            @Override
            public Set<Identifier> getProvidedSkinTypes() {
                return providedSkinTypes;
            }

            @Override
            public Optional<Identifier> getSkin(SkinType type) {
                return skins.getOrDefault(type, Optional.empty());
            }

            @Override
            public String getModel(String fallback) {
                return model.orElse(fallback);
            }
        })::get;
    }

    public void fetchSkins(GameProfile profile, SkinCallback callback) {
        cache.loadProfile(profile).thenAcceptAsync(m -> m.forEach((type, texture) -> loadTexture(type, texture, callback)), Util.getMainWorkerExecutor());
    }

    public Map<SkinType, Identifier> getTextures(GameProfile profile) {
        return loadSkinMap(cache.loadProfile(profile).getNow(Collections.emptyMap()));
    }

    private Map<SkinType, Identifier> loadSkinMap(Map<SkinType, MinecraftProfileTexture> textureMap) {
        return textureMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> loadTexture(e.getKey(), e.getValue(), SkinCallback.NOOP)));
    }

    private Identifier loadTexture(SkinType type, MinecraftProfileTexture texture, SkinCallback callback) {
        Identifier resource = new Identifier("hdskins", type.getPathName() + "/" + texture.getHash());
        AbstractTexture texObj = MinecraftClient.getInstance().getTextureManager().getOrDefault(resource, null);

        //noinspection ConstantConditions
        if (texObj != null) {
            callback.onSkinAvailable(type, resource, texture);
        } else {
            TextureLoader.loadTexture(resource, new HDPlayerSkinTexture(
                    getCachedSkinLocation(type, texture).toFile(),
                    texture.getUrl(),
                    type,
                    DefaultSkinHelper.getTexture(),
                    () -> callback.onSkinAvailable(type, resource, texture)));
        }

        return resource;
    }

    private Path getCachedSkinLocation(SkinType type, MinecraftProfileTexture texture) {
        Path location = getHDSkinsCache().resolve(type.getPathName()).resolve(texture.getHash().substring(0, 2)).resolve(texture.getHash());
        try {
            Files.createDirectories(location.getParent());
            return location;
        } catch (IOException e) {
            HDSkins.LOGGER.error("Could not create cache location for texture: {}", texture.getHash(), e);
        }
        return null;
    }

    public void clear() {
        HDSkins.LOGGER.info("Clearing local player skin cache");
        cache.clear();

        try {
            Path cachePath = getHDSkinsCache();
            if (Files.exists(cachePath)) {
                Files.walkFileTree(cachePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            HDSkins.LOGGER.warn("Could not remove cache folder", e);
        }

        SkinCacheClearCallback.EVENT.invoker().onSkinCacheCleared();
    }

    private static Path getHDSkinsCache() {
        return GamePaths.getAssetsDirectory().resolve("hd");
    }
}
