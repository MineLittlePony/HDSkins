package com.minelittlepony.hdskins.ducks;

import java.util.Map;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.util.ResourceLocation;

public interface INetworkPlayerInfo {

    Map<Type, ResourceLocation> getVanillaTextures();

    GameProfile getGameProfile();

    void reloadTextures();
}
