package com.minelittlepony.hdskins.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.resources.SkinAvailableCallback;
import com.minelittlepony.hdskins.resources.TextureLoader;
import com.minelittlepony.hdskins.resources.texture.ImageBufferDownloadHD;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.texture.Texture;
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
        offline.loadProfileAsync(profile, callback);
        MinecraftClient.getInstance().execute(() -> callback.accept(online.loadProfileAsync(profile)));
    }

    public void fetchSkins(GameProfile profile, SkinAvailableCallback callback) {
        supplyProfileTextures(profile, m -> m.forEach((type, pp) -> loadTexture(type, pp, callback)));
    }

    public Map<SkinType, Identifier> getTextures(GameProfile profile) {
        Map<SkinType, Identifier> map = new HashMap<>();

        for (Map.Entry<SkinType, MinecraftProfileTexture> e : online.loadProfileAsync(profile).entrySet()) {
            map.put(e.getKey(), loadTexture(e.getKey(), e.getValue(), null));
        }

        return map;
    }

    private Identifier loadTexture(SkinType type, MinecraftProfileTexture texture, @Nullable SkinAvailableCallback callback) {
        Identifier resource = new Identifier("hdskins", type.name().toLowerCase() + "s/" + texture.getHash());
        Texture texObj = MinecraftClient.getInstance().getTextureManager().getTexture(resource);

        //noinspection ConstantConditions
        if (texObj != null) {
            if (callback != null) {
                callback.onSkinAvailable(type, resource, texture);
            }
        } else {
            TextureLoader.loadTexture(resource, new PlayerSkinTexture(
                    getCachedSkinLocation(type, texture).toFile(),
                    texture.getUrl(),
                    DefaultSkinHelper.getTexture(),
                    new ImageBufferDownloadHD(type, () -> {
                        if (callback != null) {
                            callback.onSkinAvailable(type, resource, texture);
                        }
                    })));
        }

        return resource;
    }

    public void clear() {
        try {
            Files.deleteIfExists(getHDSkinsCache());
        } catch (IOException e) {
            HDSkins.logger.warn(e);
        }
        offline.clear();
        online.clear();
    }
}
