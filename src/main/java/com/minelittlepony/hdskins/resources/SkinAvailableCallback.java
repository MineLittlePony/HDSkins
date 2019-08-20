package com.minelittlepony.hdskins.resources;

import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.util.Identifier;

public interface SkinAvailableCallback {

    SkinAvailableCallback NOOP = (t, i, tex) -> {};

    void onSkinAvailable(SkinType type, Identifier id, MinecraftProfileTexture texture);
}
