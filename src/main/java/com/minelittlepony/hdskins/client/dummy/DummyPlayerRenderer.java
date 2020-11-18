package com.minelittlepony.hdskins.client.dummy;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.Lazy;
import net.minecraft.util.math.BlockPos;

import org.lwjgl.opengl.GL11;

import com.minelittlepony.common.client.gui.OutsideWorldRenderer;

class DummyPlayerRenderer {

    public static boolean flipReality;

    private static final Lazy<ClientPlayerEntity> NULL_PLAYER = new Lazy<>(() -> new ClientPlayerEntity(
            MinecraftClient.getInstance(),
            DummyWorld.INSTANCE,
            DummyWorld.INSTANCE.getNetHandler(),
            new StatHandler(),
            new ClientRecipeBook(), false, false
    ));

    static void wrap(Runnable action) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean inGame = client.player != null;

        OutsideWorldRenderer.configure(DummyWorld.INSTANCE);
        try {
            if (!inGame) {
                client.player = NULL_PLAYER.get();
            }
            action.run();
        } finally {
            if (!inGame) {
                client.player = null;
            }
        }
    }

    static class BedHead extends BedBlockEntity {
        public static BedHead instance = new BedHead();

        public BedHead() {
            super(BlockPos.ORIGIN, Blocks.RED_BED.getDefaultState());
        }

        public void render(Entity entity, MatrixStack stack, VertexConsumerProvider renderContext) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            stack.push();

            stack.scale(-1, 1, flipReality ? -1 : 1);
            stack.translate(-0.5, 0, 0);

            OutsideWorldRenderer.configure(entity.getEntityWorld())
                .get(this)
                .render(this, 1, stack, renderContext, 0xF000F0, OverlayTexture.DEFAULT_UV);

            stack.pop();
            GL11.glPopAttrib();
        }
    }

    public static class MrBoaty extends BoatEntity {
        public static MrBoaty instance = new MrBoaty();

        public MrBoaty() {
            super(EntityType.BOAT, DummyWorld.INSTANCE);
        }

        public void render(MatrixStack stack, VertexConsumerProvider renderContext) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            stack.push();

            @SuppressWarnings("unchecked")
            EntityRenderer<BoatEntity> render = (EntityRenderer<BoatEntity>)MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(this);

            render.render(this, 0, 0, stack, renderContext, 0xF000F0);

            stack.pop();
            GL11.glPopAttrib();
        }
    }
}
