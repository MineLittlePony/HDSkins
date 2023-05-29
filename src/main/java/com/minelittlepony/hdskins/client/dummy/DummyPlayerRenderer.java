package com.minelittlepony.hdskins.client.dummy;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.minelittlepony.common.client.gui.OutsideWorldRenderer;

public class DummyPlayerRenderer {

    private static final Supplier<CompletableFuture<ClientPlayerEntity>> FUTURE_NULL_PLAYER = Suppliers.memoize(() -> {
        return DummyWorld.FUTURE_INSTANCE.get().thenApplyAsync(w -> {
            return new ClientPlayerEntity(
                    MinecraftClient.getInstance(),
                    w,
                    DummyNetworkHandler.INSTANCE.get(),
                    new StatHandler(),
                    new ClientRecipeBook(), false, false
            );
        }, MinecraftClient.getInstance());
    });

    public static void wrap(Runnable action) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean inGame = client.player != null;

        try {
            if (!inGame) {
                ClientPlayerEntity player = FUTURE_NULL_PLAYER.get().getNow(null);

                if (player == null) {
                    return;
                }

                client.player = player;
                client.world = (ClientWorld)player.getWorld();
                OutsideWorldRenderer.configure(client.world);
            }

            action.run();
        } catch (Throwable ignored) {
            ignored.printStackTrace();
        } finally {
            if (!inGame) {
                client.world = null;
                client.player = null;
                client.interactionManager = null;
                client.cameraEntity = null;
            }
        }
    }

    static void renderBlockState(World world, BlockState state, MatrixStack stack, VertexConsumerProvider renderContext) {
        BlockRenderManager blockRenderer = MinecraftClient.getInstance().getBlockRenderManager();
        blockRenderer.getModelRenderer().render(world, blockRenderer.getModel(state),
                state, BlockPos.ORIGIN, stack,
                renderContext.getBuffer(RenderLayers.getMovingBlockLayer(state)),
                false, Random.create(),
                state.getRenderingSeed(BlockPos.ORIGIN), OverlayTexture.DEFAULT_UV);
    }

    static class BedHead extends BedBlockEntity {
        public static BedHead instance = new BedHead();

        public BedHead() {
            super(BlockPos.ORIGIN, Blocks.RED_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.HEAD)
                    .with(BedBlock.FACING, Direction.SOUTH));
        }

        public void render(Entity entity, MatrixStack stack, VertexConsumerProvider renderContext) {
            stack.push();
            stack.translate(-0.5, 0, 0);

            World world = entity.getEntityWorld();

            BlockEntityRenderer<BedHead> renderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(this);

            if (renderer != null) {
                renderer.render(this, 1, stack, renderContext, 0xF000F0, OverlayTexture.DEFAULT_UV);
            } else {
                BlockState state = getCachedState();
                renderBlockState(world, state, stack, renderContext);
                stack.translate(0, 0, -1);
                renderBlockState(world, state.with(BedBlock.PART, BedPart.FOOT), stack, renderContext);
            }

            stack.pop();
        }
    }

    public static class MrBoaty extends BoatEntity {
        public MrBoaty(World world) {
            super(EntityType.BOAT, world);
        }

        public void render(MatrixStack stack, VertexConsumerProvider renderContext) {
            stack.push();

            EntityRenderer<? super MrBoaty> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(this);

            if (renderer != null) {
                renderer.render(this, 0, 0, stack, renderContext, 0xF000F0);
            }

            stack.pop();
        }
    }
}
