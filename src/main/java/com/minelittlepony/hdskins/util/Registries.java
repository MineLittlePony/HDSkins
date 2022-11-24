package com.minelittlepony.hdskins.util;

import com.mojang.serialization.Lifecycle;

import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public interface Registries {
    static <T> Registry<T> createDefaulted(Identifier id, String def) {
        return new SimpleDefaultedRegistry<>(def, RegistryKey.ofRegistry(id), Lifecycle.stable(), true) {
            public RegistryEntry.Reference<T> set(int i, RegistryKey<T> registryKey, T object, Lifecycle lifecycle) {
                createEntry(object);
                return super.set(i, registryKey, object, lifecycle);
            }
        };
    }
}