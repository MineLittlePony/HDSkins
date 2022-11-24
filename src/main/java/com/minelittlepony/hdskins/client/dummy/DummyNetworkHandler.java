package com.minelittlepony.hdskins.client.dummy;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.base.Suppliers;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientDynamicRegistryType;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.telemetry.TelemetrySender;
import net.minecraft.client.util.telemetry.WorldSession;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.registry.*;
import net.minecraft.resource.*;

class DummyNetworkHandler extends ClientPlayNetworkHandler {
    public static final Supplier<DummyNetworkHandler> INSTANCE = Suppliers.memoize(() -> {
        return new DummyNetworkHandler(new GameProfile(null, "dumdum"));
    });

    private CombinedDynamicRegistries<ClientDynamicRegistryType> combinedDynamicRegistries = ClientDynamicRegistryType.createCombinedDynamicRegistries();

    private DummyNetworkHandler(GameProfile profile) {
        super(MinecraftClient.getInstance(),
                null,
                new ClientConnection(NetworkSide.CLIENTBOUND),
                null,
                profile,
                new WorldSession(TelemetrySender.NOOP, false, null)
        );


        LifecycledResourceManagerImpl manager = new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA, List.of(new VanillaDataPackProvider().getResourcePack()));

        combinedDynamicRegistries = combinedDynamicRegistries.with(ClientDynamicRegistryType.REMOTE,
                RegistryLoader.load(manager, combinedDynamicRegistries.getCombinedRegistryManager(), Stream.concat(
                        RegistryLoader.DYNAMIC_REGISTRIES.stream(),
                        RegistryLoader.DIMENSION_REGISTRIES.stream()
                ).toList())
        );
    }

    @Override
    public DynamicRegistryManager getRegistryManager() {
        return combinedDynamicRegistries.getCombinedRegistryManager();
    }
}
