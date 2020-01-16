package com.minelittlepony.hdskins.client;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@FunctionalInterface
public interface SkinCacheClearCallback {

    Event<SkinCacheClearCallback> EVENT = EventFactory.createArrayBacked(SkinCacheClearCallback.class, listeners -> () -> {
        for (SkinCacheClearCallback event : listeners) {
            event.onSkinCacheCleared();
        }
    });

    void onSkinCacheCleared();
}
