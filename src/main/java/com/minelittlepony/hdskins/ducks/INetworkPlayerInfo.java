package com.minelittlepony.hdskins.ducks;

import java.util.Map;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.util.Identifier;

public interface INetworkPlayerInfo {

    Map<Type, Identifier> getVanillaTextures();

    GameProfile getGameProfile();
}
