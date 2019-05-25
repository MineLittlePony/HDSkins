package com.minelittlepony.hdskins.resources;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Texture;
import net.minecraft.util.Identifier;

public class TextureLoader {

    private static MinecraftClient mc = MinecraftClient.getInstance();

    public static void loadTexture(final Identifier textureLocation, final Texture texture) {
        mc.execute(() -> mc.getTextureManager().registerTexture(textureLocation, texture));
    }
}
