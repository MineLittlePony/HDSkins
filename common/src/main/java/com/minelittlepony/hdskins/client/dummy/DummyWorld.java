package com.minelittlepony.hdskins.client.dummy;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.DummyProfiler;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;

public class DummyWorld extends ClientWorld {

    private BlockState worldBlockState = Blocks.ACACIA_STAIRS.getDefaultState();

    public DummyWorld() {
        super(null, new LevelInfo(0, GameMode.NOT_SET, false, false, LevelGeneratorType.DEFAULT),
                DimensionType.OVERWORLD,
                0,
                DummyProfiler.INSTANCE,
                null);
    }

    public void fillWith(BlockState state) {
        worldBlockState = state;
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return true;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return worldBlockState;
    }

    @Override
    public void playLevelEvent(PlayerEntity arg0, int arg1, BlockPos arg2, int arg3) {
    }
}
