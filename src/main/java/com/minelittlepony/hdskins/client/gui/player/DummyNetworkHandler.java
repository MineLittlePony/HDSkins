package com.minelittlepony.hdskins.client.gui.player;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.base.Suppliers;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientDynamicRegistryType;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.session.telemetry.TelemetrySender;
import net.minecraft.client.session.telemetry.WorldSession;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.registry.*;
import net.minecraft.resource.*;
import net.minecraft.resource.featuretoggle.FeatureSet;

class DummyNetworkHandler {
    public static final Supplier<ClientPlayNetworkHandler> INSTANCE = Suppliers.memoize(() -> {
        return new ClientPlayNetworkHandler(MinecraftClient.getInstance(), new ClientConnection(NetworkSide.CLIENTBOUND), new ClientConnectionState(
                new GameProfile(null, "dumdum"),
                new WorldSession(TelemetrySender.NOOP, false, null, null),
                RegistryLoader.load(new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA, List.of()), ClientDynamicRegistryType.createCombinedDynamicRegistries().getCombinedRegistryManager(), Stream.concat(
                        RegistryLoader.DYNAMIC_REGISTRIES.stream(),
                        RegistryLoader.DIMENSION_REGISTRIES.stream()
                ).toList()),
                FeatureSet.empty(),
                (String)null,
                (ServerInfo)null,
                (Screen)null
        ));
    });
}
