package com.minelittlepony.hdskins.client.dummy;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.RequiredTagListRegistry;
import net.minecraft.util.Lazy;

class DummyNetworkHandler extends ClientPlayNetworkHandler {
    public static final Lazy<DummyNetworkHandler> INSTANCE = new Lazy<>(() -> {
        return new DummyNetworkHandler(new GameProfile(null, "dumdum"));
    });

    public DummyNetworkHandler(GameProfile profile) {
        super(MinecraftClient.getInstance(),
                null,
                new ClientConnection(NetworkSide.CLIENTBOUND), profile);
        try {
            BlockTags.CLIMBABLE.contains(Blocks.LADDER);
        } catch (IllegalStateException ignored) {
            RequiredTagListRegistry.clearAllTags();
        }
    }
}
