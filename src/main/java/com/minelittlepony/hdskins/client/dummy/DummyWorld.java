package com.minelittlepony.hdskins.client.dummy;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;

public class DummyWorld extends ClientWorld {
    public static final DummyWorld INSTANCE = new DummyWorld(new DummyNetworkHandler(new GameProfile(null, "dumdum")));

    private BlockState worldBlockState = Blocks.AIR.getDefaultState();

    private final WorldChunk chunk = new EmptyChunk(this, new ChunkPos(0, 0));
    private final ClientChunkManager chunkManager = new ClientChunkManager(this, 0) {
        private final LightingProvider lighting = new LightingProvider(this, false, false) {
            @Override
            public int getLight(BlockPos pos, int ambientDarkness) { return 15; }
        };

        @Override
        public WorldChunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create) { return chunk; }

        @Override
        public LightingProvider getLightingProvider() { return lighting; }
    };

    private final ClientPlayNetworkHandler net;

    private DummyWorld(ClientPlayNetworkHandler net) {
        super(net,
                new ClientWorld.Properties(Difficulty.NORMAL, false, true),
                World.OVERWORLD,
                net.getRegistryManager().getDimensionTypes().getOrThrow(DimensionType.OVERWORLD_REGISTRY_KEY),
                0,
                MinecraftClient.getInstance()::getProfiler,
                MinecraftClient.getInstance().worldRenderer,
                true,
                0);
        this.net = net;
    }

    public ClientPlayNetworkHandler getNetHandler() {
        return net;
    }

    public DummyWorld fillWith(BlockState state) {
        worldBlockState = state;

        return this;
    }

    @Override
    public ClientChunkManager getChunkManager() {
        return chunkManager;
    }

    @Override
    public WorldChunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        return chunk;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return worldBlockState;
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }
}
