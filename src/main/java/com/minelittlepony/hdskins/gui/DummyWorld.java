package com.minelittlepony.hdskins.gui;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.fluid.Fluid;
import net.minecraft.init.Blocks;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.tags.NetworkTagManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.dimension.OverworldDimension;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;

public class DummyWorld extends World {

    public static final World INSTANCE = new DummyWorld();

    private final Chunk chunk = new EmptyChunk(this, 0, 0);
    private final Scoreboard scoreboard = new Scoreboard();
    private final RecipeManager recipeManager = new RecipeManager();
    private final NetworkTagManager tags = new NetworkTagManager();

    private DummyWorld() {
        super(new SaveHandlerMP(),
                null,
                new WorldInfo(new WorldSettings(0, GameType.NOT_SET, false, false, WorldType.DEFAULT), "MpServer"),
                new OverworldDimension(),
                Minecraft.getInstance().profiler,
                true);
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return new ChunkProviderClient(this);
    }

    @Override
    public boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
        return true;
    }

    @Override
    public ITickList<Block> getPendingBlockTicks() {
        return EmptyTickList.get();
    }

    @Override
    public ITickList<Fluid> getPendingFluidTicks() {
        return EmptyTickList.get();
    }

    @Override
    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunk;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public float getBrightness(BlockPos pos) {
        return 1;
    }

    @Override
    public BlockPos getSpawnPoint() {
        return BlockPos.ORIGIN;
    }

    @Override
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    @Override
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    @Override
    public NetworkTagManager getTags() {
        return tags;
    }
}
