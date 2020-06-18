package com.minelittlepony.hdskins.core;

import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.texture.TextureManager;

import java.util.Map;
import java.util.UUID;

public abstract class PrivateFields {

    public final ObfHelper<ClientPlayNetworkHandler, Map<UUID, PlayerListEntry>> playerListEntries =
            factory().get(ClientPlayNetworkHandler.class, ClientPlayNetworkHandler_playerListEntries(), Map.class);
    public final ObfHelper<PlayerSkinProvider, LoadingCache<GameProfile, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>>> skinCache =
            factory().get(PlayerSkinProvider.class, PlayerSkinProvider_skinCache(), LoadingCache.class);
    public final ObfHelper<PlayerSkinProvider, TextureManager> textureManager =
            factory().get(PlayerSkinProvider.class, PlayerSkinProvider_textureManager(), TextureManager.class);

    protected abstract ObfHelperFactory factory();

    protected abstract String ClientPlayNetworkHandler_playerListEntries();

    protected abstract String PlayerSkinProvider_skinCache();

    protected abstract String PlayerSkinProvider_textureManager();

    public interface ObfHelperFactory {
        <Owner, Desc> ObfHelper<Owner, Desc> get(Class<Owner> owner, String name, Class<?> desc);
    }
}
