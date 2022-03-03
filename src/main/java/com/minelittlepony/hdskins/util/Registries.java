package com.minelittlepony.hdskins.util;

import com.mojang.serialization.Lifecycle;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

public interface Registries {
    static <T> Registry<T> createDefaulted(Identifier id, String def) {
        return new DefaultedRegistry<>(def, RegistryKey.ofRegistry(id), Lifecycle.stable(), null);
    }
}