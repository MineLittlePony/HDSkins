package com.minelittlepony.hdskins.profile;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.util.Identifier;

public interface SkinCallback {

    SkinCallback NOOP = (t, i, tex) -> {};

    void onSkinAvailable(SkinType type, Identifier id, MinecraftProfileTexture texture);

    default SkinCallback andThen(Runnable second) {
        return andThen((t, i, tex) -> second.run());
    }

    default SkinCallback andThen(SkinCallback second) {
        SkinCallback first = this;
        return (t, i, tex) -> {
            first.onSkinAvailable(t, i, tex);
            second.onSkinAvailable(t, i, tex);
        };
    }
}
