package com.minelittlepony.hdskins.profile;

import java.util.Collection;

import javax.annotation.Nullable;

import com.google.gson.TypeAdapter;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

public interface SkinType extends Comparable<SkinType> {

    SkinType UNKNOWN = register(() -> "UNKNOWN");
    SkinType SKIN = forVanilla(MinecraftProfileTexture.Type.SKIN);
    SkinType CAPE = forVanilla(MinecraftProfileTexture.Type.CAPE);
    SkinType ELYTRA = forVanilla(MinecraftProfileTexture.Type.ELYTRA);

    String name();

    default boolean isKnown() {
        return this != UNKNOWN;
    }

    default boolean isVanilla() {
        return getEnum() != null;
    }

    @Nullable
    default MinecraftProfileTexture.Type getEnum() {
        return null;
    }

    @Override
    default int compareTo(SkinType o) {
        return o.name().compareTo(name());
    }

    static TypeAdapter<SkinType> adapter() {
        return SkinTypes.ADAPTER;
    }

    static Collection<SkinType> values() {
        return SkinTypes.VALUES.values();
    }

    static SkinType register(SkinType type) {
        SkinTypes.VALUES.put(type.name(), type);
        return type;
    }

    static SkinType forVanilla(MinecraftProfileTexture.Type vanilla) {
        return SkinTypes.forVanilla(vanilla);
    }
}
