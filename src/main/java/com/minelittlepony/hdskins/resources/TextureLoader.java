package com.minelittlepony.hdskins.resources;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

public class TextureLoader {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Schedule texture loading on the main thread.
     * @param textureLocation
     * @param texture
     */
    public static void loadTexture(final Identifier textureLocation, final AbstractTexture texture) {
        mc.execute(() -> mc.getTextureManager().registerTexture(textureLocation, texture));
    }
}
