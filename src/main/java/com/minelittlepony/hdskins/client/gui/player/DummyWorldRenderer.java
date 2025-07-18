package com.minelittlepony.hdskins.client.gui.player;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.joml.Matrix4f;
import org.joml.Vector4f;

final class DummyWorldRenderer extends WorldRenderer {

    public static final Supplier<CompletableFuture<DummyWorldRenderer>> FUTURE_INSTANCE = () -> {
        return CompletableFuture.supplyAsync(DummyWorldRenderer::new, MinecraftClient.getInstance());
    };

    public DummyWorldRenderer() {
        super(MinecraftClient.getInstance(),
                MinecraftClient.getInstance().getEntityRenderDispatcher(),
                MinecraftClient.getInstance().getBlockEntityRenderDispatcher(),
                MinecraftClient.getInstance().getBufferBuilders());
    }

    @Override
    public void reload() {
        // noop
    }

    @Override
    public void render(
            ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            GpuBufferSlice fog,
            Vector4f fogColor,
            boolean shouldRenderSky
    ) {
        // noop
    }

    @Override
    public void addBuiltChunk(ChunkBuilder.BuiltChunk chunk) {
        // noop
    }

    @Override
    public void onResized(int width, int height) {
        // noop
    }

    @Override
    protected boolean canDrawEntityOutlines() {
        return false;
    }

    @Override
    public void reload(ResourceManager manager) {
        // noop
    }

    @Override
    public void tick() {
        // noop
    }

    @Override
    public void updateBlock(BlockView world, BlockPos pos, BlockState oldState, BlockState newState, int flags) {
        // noop
    }

    @Override
    public void scheduleBlockRenders(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        // noop
    }

    @Override
    public void scheduleBlockRerenderIfNeeded(BlockPos pos, BlockState old, BlockState updated) {
        // noop
    }

    @Override
    public void addParticle(ParticleEffect parameters, boolean shouldAlwaysSpawn, double x, double y, double z,
                            double velocityX, double velocityY, double velocityZ) {
        // noop
    }

    @Override
    public void addParticle(ParticleEffect parameters, boolean shouldAlwaysSpawn, boolean important, double x,
                            double y, double z, double velocityX, double velocityY, double velocityZ) {
        // noop
    }

    @Override
    public void setBlockBreakingInfo(int entityId, BlockPos pos, int stage) {
        // noop
    }

    @Override
    public boolean isRenderingReady(BlockPos pos) {
        return true;
    }
}
