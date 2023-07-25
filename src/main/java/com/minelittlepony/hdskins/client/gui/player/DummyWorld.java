package com.minelittlepony.hdskins.client.gui.player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.minelittlepony.hdskins.client.HDSkins;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.*;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.*;

public class DummyWorld extends ClientWorld {
    public static final Supplier<CompletableFuture<DummyWorld>> FUTURE_INSTANCE = Suppliers.memoize(() -> {
        return DummyWorldRenderer.FUTURE_INSTANCE.get()
                .thenApplyAsync(worldRenderer -> new DummyWorld(DummyNetworkHandler.INSTANCE.get(), worldRenderer), Util.getMainWorkerExecutor())
                .exceptionallyAsync(t -> {
            HDSkins.LOGGER.error("Could not asynchrnously load a dummy world. Falling back to on-thread construction. This may impact performance.", t);
            try {
                return new DummyWorld(DummyNetworkHandler.INSTANCE.get(), new DummyWorldRenderer());
            } catch (Throwable tt) {
                HDSkins.LOGGER.fatal("Dummy world failed spectacularly. Report this to the devs. D:", t);
                throw tt;
            }
        }, MinecraftClient.getInstance());
    });

    public static CompletableFuture<? extends ClientWorld> getOrDummyFuture() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world != null) {
            return CompletableFuture.completedFuture(client.world);
        }

        return FUTURE_INSTANCE.get();
    }

    public static void fillWith(BlockState state) {
        worldBlockState = state;
    }

    private static BlockState worldBlockState = Blocks.AIR.getDefaultState();

    private final WorldChunk chunk;
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

    private DummyWorld(ClientPlayNetworkHandler net, WorldRenderer worldRenderer) {
        super(net,
                new ClientWorld.Properties(Difficulty.NORMAL, false, true),
                World.OVERWORLD,
                net.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).entryOf(DimensionTypes.OVERWORLD),
                0,
                0,
                MinecraftClient.getInstance()::getProfiler,
                worldRenderer,
                true,
                0);
        chunk = new EmptyChunk(this, new ChunkPos(0, 0), getRegistryManager().get(Registry.BIOME_KEY).entryOf(BiomeKeys.PLAINS));
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
