package com.minelittlepony.hdskins.client.dummy;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Lazy;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.*;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;

public class DummyWorld extends ClientWorld {
    public static final Lazy<DummyWorld> INSTANCE = new Lazy<>(() -> {
        return new DummyWorld(DummyNetworkHandler.INSTANCE.get());
    });

    public static ClientWorld getOrDummy() {
        ClientWorld w = MinecraftClient.getInstance().world;
        return w == null ? INSTANCE.get() : w;
    }

    public static void fillWith(BlockState state) {
        worldBlockState = state;
    }

    private static BlockState worldBlockState = Blocks.AIR.getDefaultState();

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

    private DummyWorld(ClientPlayNetworkHandler net) {
        super(net,
                new ClientWorld.Properties(Difficulty.NORMAL, false, true),
                World.OVERWORLD,
                net.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getOrThrow(DimensionType.OVERWORLD_REGISTRY_KEY),
                0,
                MinecraftClient.getInstance()::getProfiler,
                MinecraftClient.getInstance().worldRenderer,
                true,
                0);
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
