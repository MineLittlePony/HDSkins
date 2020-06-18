package com.minelittlepony.hdskins.core;

import com.google.common.collect.ForwardingMap;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class EventHookedPlayerMap extends ForwardingMap<UUID, PlayerListEntry> {

    private final Map<UUID, PlayerListEntry> delegate;
    private final Consumer<PlayerListEntry> callback;

    public EventHookedPlayerMap(Map<UUID, PlayerListEntry> delegate, Consumer<PlayerListEntry> callback) {
        this.delegate = delegate;
        this.callback = callback;
    }

    @Override
    protected Map<UUID, PlayerListEntry> delegate() {
        return delegate;
    }

    @Override
    public PlayerListEntry put(UUID key, PlayerListEntry value) {
        callback.accept(value);
        return super.put(key, value);
    }
}
