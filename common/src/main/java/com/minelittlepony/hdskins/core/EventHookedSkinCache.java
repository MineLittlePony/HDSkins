package com.minelittlepony.hdskins.core;

import com.google.common.cache.ForwardingLoadingCache;
import com.google.common.cache.LoadingCache;
import com.minelittlepony.hdskins.skins.SkinCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class EventHookedSkinCache extends ForwardingLoadingCache<GameProfile, Map<Type, MinecraftProfileTexture>> {

    private final LoadingCache<GameProfile, Map<Type, MinecraftProfileTexture>> delegate;
    private final Supplier<SkinCache> skinCache;

    public EventHookedSkinCache(LoadingCache<GameProfile, Map<Type, MinecraftProfileTexture>> delegate, Supplier<SkinCache> skinCache) {
        this.delegate = delegate;
        this.skinCache = skinCache;
    }

    @Override
    protected LoadingCache<GameProfile, Map<Type, MinecraftProfileTexture>> delegate() {
        return delegate;
    }

    @Override
    public Map<Type, MinecraftProfileTexture> getUnchecked(GameProfile key) {
        CompletableFuture<Map<Type, MinecraftProfileTexture>> future = skinCache.get().getPayload(key)
                .thenApply(MinecraftTexturesPayload::getTextures);

        // it may take some time to get the skins.
        Map<Type, MinecraftProfileTexture> skins = super.getUnchecked(key);
        skins.putAll(future.getNow(Collections.emptyMap()));
        return skins;
    }
}