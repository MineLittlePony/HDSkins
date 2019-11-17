package com.minelittlepony.hdskins.profile;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.SkinCacheClearCallback;
import com.minelittlepony.hdskins.resources.HDPlayerSkinTexture;
import com.minelittlepony.hdskins.resources.SkinCallback;
import com.minelittlepony.hdskins.resources.TextureLoader;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

public class ProfileRepository {

    final OfflineProfileCache offline = new OfflineProfileCache(this);
    final OnlineProfileCache online = new OnlineProfileCache(this);

    final HDSkins hd;

    public ProfileRepository(HDSkins hd) {
        this.hd = hd;
    }

    public Path getHDSkinsCache() {
        return GamePaths.getAssetsDirectory().resolve("hd");
    }

    private Path getCachedSkinLocation(SkinType type, MinecraftProfileTexture texture) {
        String skinDir = type.name().toLowerCase() + "s/";

        return getHDSkinsCache().resolve(skinDir + texture.getHash().substring(0, 2)).resolve(texture.getHash());
    }

    private void supplyProfileTextures(GameProfile profile, Consumer<Map<SkinType, MinecraftProfileTexture>> callback) {
        offline.loadProfile(profile).thenAcceptAsync(callback, MinecraftClient.getInstance());
        online.loadProfile(profile).thenAcceptAsync(callback, MinecraftClient.getInstance());
    }

    public void fetchSkins(GameProfile profile, SkinCallback callback) {
        supplyProfileTextures(profile, m -> m.forEach((type, pp) -> loadTexture(type, pp, callback)));
    }

    public Map<SkinType, Identifier> getTextures(GameProfile profile) {
        return online.loadProfile(profile).getNow(Collections.emptyMap()).entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> loadTexture(e.getKey(), e.getValue(), SkinCallback.NOOP))
        );
    }

    private Identifier loadTexture(SkinType type, MinecraftProfileTexture texture, SkinCallback callback) {
        Identifier resource = new Identifier("hdskins", type.name().toLowerCase() + "s/" + texture.getHash());
        AbstractTexture texObj = MinecraftClient.getInstance().getTextureManager().getTexture(resource);

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

    public void clear() {
        HDSkins.logger.info("Clearing local player skin cache");
        offline.clear();
        online.clear();
        SkinCacheClearCallback.EVENT.invoker().onSkinCacheCleared();
    }
}
