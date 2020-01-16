package com.minelittlepony.hdskins.client.resources;

import com.google.common.collect.Maps;
import com.minelittlepony.hdskins.skins.TexturePayload;
import com.minelittlepony.hdskins.skins.SkinType;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.util.Identifier;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Manager for fetching preview textures. This ensures that multiple calls
 * to the skin net aren't done when fetching preview textures.
 */
public class PreviewTextureManager {

    private final Map<SkinType, MinecraftProfileTexture> textures;

    public PreviewTextureManager(TexturePayload payload) {
        this.textures = payload.getTextures();
    }

    @Nullable
    public PreviewTexture getPreviewTexture(Identifier location, SkinType type, Identifier def, SkinCallback callback) {

        if (!textures.containsKey(type)) {
            return null;
        }

        MinecraftProfileTexture texture = textures.get(type);

        PreviewTexture skinTexture = PreviewTexture.create(texture, type, def, () -> {
            callback.onSkinAvailable(type, location, new MinecraftProfileTexture(texture.getUrl(), Maps.newHashMap()));
        });

        TextureLoader.loadTexture(location, skinTexture);

        return skinTexture;
    }
}
