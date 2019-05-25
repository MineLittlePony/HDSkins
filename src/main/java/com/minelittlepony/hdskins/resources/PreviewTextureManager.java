package com.minelittlepony.hdskins.resources;

import com.google.common.collect.Maps;
import com.minelittlepony.hdskins.resources.texture.ImageBufferDownloadHD;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;

import net.minecraft.client.texture.PlayerSkinProvider.SkinTextureAvailableCallback;
import net.minecraft.util.Identifier;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Manager for fetching preview textures. This ensures that multiple calls
 * to the skin net aren't done when fetching preview textures.
 */
public class PreviewTextureManager {

    private final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures;

    public PreviewTextureManager(MinecraftTexturesPayload payload) {
        this.textures = payload.getTextures();
    }

    @Nullable
    public PreviewTexture getPreviewTexture(Identifier location, MinecraftProfileTexture.Type type, Identifier def, @Nullable SkinTextureAvailableCallback callback) {
        if (!textures.containsKey(type)) {
            return null;
        }

        MinecraftProfileTexture texture = textures.get(type);

        PreviewTexture skinTexture = new PreviewTexture(texture, def, new ImageBufferDownloadHD(type, () -> {
            if (callback != null) {
                callback.onSkinTextureAvailable(type, location, new MinecraftProfileTexture(texture.getUrl(), Maps.newHashMap()));
            }
        }));

        TextureLoader.loadTexture(location, skinTexture);

        return skinTexture;
    }
}
