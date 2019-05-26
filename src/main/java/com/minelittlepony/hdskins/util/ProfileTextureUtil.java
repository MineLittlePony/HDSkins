package com.minelittlepony.hdskins.util;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage;

import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Map;

public class ProfileTextureUtil {

    private static Field metadata = FieldUtils.getDeclaredField(MinecraftProfileTexture.class, "metadata", true);

    @SuppressWarnings("unchecked")
    @Nullable
    public static Map<String, String> getMetadata(MinecraftProfileTexture texture) {
        try {
            return (Map<String, String>) FieldUtils.readField(metadata, texture);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to read metadata field", e);
        }
    }

    public static void setMetadata(MinecraftProfileTexture texture, Map<String, String> meta) {
        try {
            FieldUtils.writeField(metadata, texture, meta);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to write metadata field", e);
        }
    }

    public static NativeImage getDynamicBufferedImage(int width, int height, NativeImageBackedTexture texture) {
        NativeImage image = new NativeImage(16, 16, true);
        image.copyFrom(texture.getImage());
        return image;
    }
}
