package com.minelittlepony.hdskins.fabric.callback;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public interface ClientLogInCallback {
    Event<ClientLogInCallback> EVENT = EventFactory.createArrayBacked(ClientLogInCallback.class, listeners -> handler -> {
        for (ClientLogInCallback callback : listeners) {
            callback.onClientLogIn(handler);
        }
    });

    void onClientLogIn(ClientPlayNetworkHandler handler);
}
