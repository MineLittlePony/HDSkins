package com.minelittlepony.hdskins.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.client.resources.HDPlayerSkinTexture;
import com.minelittlepony.hdskins.client.resources.SkinCallback;
import com.minelittlepony.hdskins.client.resources.TextureLoader;
import com.minelittlepony.hdskins.skins.SkinServerList;
import com.minelittlepony.hdskins.skins.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ProfileRepository {

    /**
     * Cache used for getting the player skull block texture.
     */
    private final LoadingCache<GameProfile, CompletableFuture<Map<SkinType, Identifier>>> profiles = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::loadServerTextures));

    public Path getHDSkinsCache() {
        return GamePaths.getAssetsDirectory().resolve("hd");
    }

    private Path getCachedSkinLocation(SkinType type, MinecraftProfileTexture texture) {
        String skinDir = type.name().toLowerCase() + "s/";

        return getHDSkinsCache().resolve(skinDir + texture.getHash().substring(0, 2)).resolve(texture.getHash());
    }

    private CompletableFuture<Map<SkinType, Identifier>> loadServerTextures(GameProfile profile) {
        final SkinServerList serverList = HDSkins.getInstance().getSkinServerList();
        return serverList.loadProfileTextures(profile)
                .thenApply(textures -> {
                    Map<SkinType, Identifier> map = new HashMap<>();
                    for (Map.Entry<SkinType, MinecraftProfileTexture> entry : textures.entrySet()) {
                        loadTexture(entry.getKey(), entry.getValue(), (type, id, texture) -> map.put(type, id));
                    }
                    return map;
                });
    }

    public Map<SkinType, Identifier> getTextures(GameProfile profile) {
        return profiles.getUnchecked(profile).getNow(Collections.emptyMap());
    }

    public void loadTexture(SkinType type, MinecraftProfileTexture texture, SkinCallback callback) {
        Identifier resource = new Identifier("hdskins", type.name().toLowerCase() + "s/" + texture.getHash());
        AbstractTexture texObj = MinecraftClient.getInstance().getTextureManager().getTexture(resource);

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
    }

    public void clear() {
        HDSkins.logger.info("Clearing local player skin cache");
        profiles.invalidateAll();
        SkinCacheClearCallback.EVENT.invoker().onSkinCacheCleared();
    }
}
