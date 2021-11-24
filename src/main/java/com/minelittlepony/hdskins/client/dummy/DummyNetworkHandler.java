package com.minelittlepony.hdskins.client.dummy;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.authlib.GameProfile;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.RequiredTagListRegistry;

class DummyNetworkHandler extends ClientPlayNetworkHandler {
    public static final Supplier<DummyNetworkHandler> INSTANCE = Suppliers.memoize(() -> {
        return new DummyNetworkHandler(new GameProfile(null, "dumdum"));
    });

    private DummyNetworkHandler(GameProfile profile) {
        super(MinecraftClient.getInstance(),
                null,
                new ClientConnection(NetworkSide.CLIENTBOUND),
                profile,
                MinecraftClient.getInstance().createTelemetrySender()
        );
        try {
            BlockTags.CLIMBABLE.contains(Blocks.LADDER);
        } catch (IllegalStateException ignored) {
            RequiredTagListRegistry.clearAllTags();
        }
    }
}
