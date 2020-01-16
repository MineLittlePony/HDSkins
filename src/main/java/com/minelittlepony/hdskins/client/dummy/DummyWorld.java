package com.minelittlepony.hdskins.client.dummy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.DummyClientTickScheduler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.tag.RegistryTagManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;

public class DummyWorld extends World {

    public static final DummyWorld INSTANCE = new DummyWorld();

    private final WorldChunk chunk = new EmptyChunk(this, new ChunkPos(0, 0));
    private final Scoreboard scoreboard = new Scoreboard();
    private final RecipeManager recipeManager = new RecipeManager();
    private final RegistryTagManager tags = new RegistryTagManager();

    private BlockState worldBlockState = Blocks.ACACIA_STAIRS.getDefaultState();

    private DummyWorld() {
        super(new LevelProperties(new LevelInfo(0, GameMode.NOT_SET, false, false, LevelGeneratorType.DEFAULT), "MpServer"),
                DimensionType.OVERWORLD,
                (w, dim) -> null,
                MinecraftClient.getInstance().getProfiler(),
                true);
    }

    public DummyWorld fillWith(BlockState state) {
        worldBlockState = state;

        return this;
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return true;
    }

    @Override
    public TickScheduler<Block> getBlockTickScheduler() {
        return DummyClientTickScheduler.get();
    }

    @Override
    public TickScheduler<Fluid> getFluidTickScheduler() {
        return DummyClientTickScheduler.get();
    }

    @Override
    public WorldChunk getChunk(int chunkX, int chunkZ) {
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

    @Override
    public float getBrightness(BlockPos pos) {
        return 16;
    }

    @Override
    public BlockPos getSpawnPos() {
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
    public RegistryTagManager getTagManager() {
        return tags;
    }

    @Override
    public void playLevelEvent(PlayerEntity arg0, int arg1, BlockPos arg2, int arg3) {
    }

    @Override
    public List<? extends PlayerEntity> getPlayers() {
        return new ArrayList<>();
    }

    @Override
    public Entity getEntityById(int arg0) {
        return null;
    }

    @Override
    public MapState getMapState(String arg0) {
        return null;
    }

    @Override
    public int getNextMapId() {
        return 0;
    }

    @Override
    public void playSound(PlayerEntity arg0, double arg1, double arg2, double arg3, SoundEvent arg4, SoundCategory arg5, float arg6, float arg7) {
    }

    @Override
    public void playSoundFromEntity(PlayerEntity arg0, Entity arg1, SoundEvent arg2, SoundCategory arg3, float arg4, float arg5) {
    }

    @Override
    public void putMapState(MapState arg0) {
    }

    @Override
    public void updateListeners(BlockPos arg0, BlockState arg1, BlockState arg2, int arg3) {
    }

    @Override
    public Biome getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
        return Biomes.OCEAN;
    }

    @Override
    public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {
    }
}
