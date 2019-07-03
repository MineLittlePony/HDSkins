package com.minelittlepony.hdskins.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.resources.TextureLoader;
import com.minelittlepony.hdskins.resources.texture.ImageBufferDownloadHD;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.client.texture.PlayerSkinProvider.SkinTextureAvailableCallback;
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

    private Path getCachedSkinLocation(Type type, MinecraftProfileTexture texture) {
        String skinDir = type.toString().toLowerCase() + "s/";

        return getHDSkinsCache().resolve(skinDir + texture.getHash().substring(0, 2)).resolve(texture.getHash());
    }

    private void supplyProfileTextures(GameProfile profile, Consumer<Map<Type, MinecraftProfileTexture>> callback) {
        offline.loadProfileAsync(profile, callback);
        online.loadProfileAsync(profile).thenAcceptAsync(callback, MinecraftClient.getInstance()::execute);
    }

    public void fetchSkins(GameProfile profile, SkinTextureAvailableCallback callback) {
        supplyProfileTextures(profile, m -> m.forEach((type, pp) -> loadTexture(type, pp, callback)));
    }

    public Map<Type, Identifier> getTextures(GameProfile profile) {
        Map<Type, Identifier> map = new HashMap<>();

        for (Map.Entry<Type, MinecraftProfileTexture> e : online.loadProfileAsync(profile).getNow(Collections.emptyMap()).entrySet()) {
            map.put(e.getKey(), loadTexture(e.getKey(), e.getValue(), null));
        }

        return map;
    }

    private Identifier loadTexture(Type type, MinecraftProfileTexture texture, @Nullable SkinTextureAvailableCallback callback) {
        Identifier resource = new Identifier("hdskins", type.toString().toLowerCase() + "s/" + texture.getHash());
        Texture texObj = MinecraftClient.getInstance().getTextureManager().getTexture(resource);

        //noinspection ConstantConditions
        if (texObj != null) {
            if (callback != null) {
                callback.onSkinTextureAvailable(type, resource, texture);
            }
        } else {
            TextureLoader.loadTexture(resource, new PlayerSkinTexture(
                    getCachedSkinLocation(type, texture).toFile(),
                    texture.getUrl(),
                    DefaultSkinHelper.getTexture(),
                    new ImageBufferDownloadHD(type, () -> {
                        if (callback != null) {
                            callback.onSkinTextureAvailable(type, resource, texture);
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
