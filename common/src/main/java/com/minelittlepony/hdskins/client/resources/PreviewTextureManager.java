package com.minelittlepony.hdskins.client.resources;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Manager for fetching preview textures. This ensures that multiple calls
 * to the skin net aren't done when fetching preview textures.
 */
public class PreviewTextureManager {

    private final Map<Type, MinecraftProfileTexture> textures;

    public PreviewTextureManager(Map<Type, MinecraftProfileTexture> textures) {
        this.textures = textures;
    }

    @Nullable
    public AbstractTexture loadServerTexture(Identifier location, Type type, Identifier def, SkinCallback callback) {
        return null;
    }

}
