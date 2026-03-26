package com.minelittlepony.hdskins.profile;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.resources.Identifier;

public interface SkinCallback {

    SkinCallback NOOP = (_, _, _) -> {};

    void onSkinAvailable(SkinType type, Identifier id, MinecraftProfileTexture texture);

    default SkinCallback andThen(Runnable second) {
        return andThen((_, _, _) -> second.run());
    }

    default SkinCallback andThen(SkinCallback second) {
        SkinCallback first = this;
        if (first == NOOP) return second;
        if (second == NOOP) return first;
        return (t, i, tex) -> {
            first.onSkinAvailable(t, i, tex);
            second.onSkinAvailable(t, i, tex);
        };
    }
}
