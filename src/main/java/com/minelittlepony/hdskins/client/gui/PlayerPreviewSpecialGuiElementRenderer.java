package com.minelittlepony.hdskins.client.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BedBlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.BoatEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;

public class PlayerPreviewSpecialGuiElementRenderer extends SpecialGuiElementRenderer<PlayerPreviewSpecialGuiElementRenderer.Element> {
    public static final Logger LOGGER = LogManager.getLogger();
    private static final BoatEntityRenderState BOAT_STATE = new BoatEntityRenderState() {{
        entityType = EntityType.OAK_BOAT;
        light = LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE;
    }};
    private static final BedBlockEntityRenderState BED_STATE = new BedBlockEntityRenderState() {{
        type = BlockEntityType.BED;
        dyeColor = DyeColor.RED;
        blockState = Blocks.RED_BED.getDefaultState().with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, Direction.SOUTH);
        facing = Direction.WEST;
    }};

    public PlayerPreviewSpecialGuiElementRenderer(SpecialGuiElementRegistry.Context context) {
        super(context.vertexConsumers());
    }

    @Override
    protected String getName() {
        return "hdskins_player_preview";
    }

    @Override
    public Class<Element> getElementClass() {
        return Element.class;
    }

    @Override
    protected void render(Element state, MatrixStack stack) {
        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);
        RenderDispatcher renderDispatcher = MinecraftClient.getInstance().gameRenderer.getEntityRenderDispatcher();
        CameraRenderState camera = new CameraRenderState();
        BlockEntityRenderManager blocks = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();
        EntityRenderManager entities = MinecraftClient.getInstance().getEntityRenderDispatcher();
        OrderedRenderCommandQueue queue = renderDispatcher.getQueue();

        Vector3f pos = state.translation();

        MatrixStack matrices = new MatrixStack();
        matrices.peek().copy(stack.peek());
        try {
            matrices.push();
            matrices.translate(pos.x, pos.y, pos.z);
            matrices.multiply(state.rotation());

            if (state.state.isInPose(EntityPose.SLEEPING)) {
                matrices.push();
                matrices.translate(0, -1.22, -0.5);
                BED_STATE.headPart = false;
                BED_STATE.lightmapCoordinates = LightmapTextureManager.MAX_LIGHT_COORDINATE;
                BED_STATE.dyeColor = DyeColor.RED;

                try {
                    blocks.render(BED_STATE, matrices, queue, camera);
                } catch (Throwable t) {
                    handleError("bed 1", t);
                }

                BED_STATE.headPart = true;
                matrices.translate(-1, 0, 0);

                try {
                    blocks.render(BED_STATE, matrices, queue, camera);
                } catch (Throwable t) {
                    handleError("bed 2", t);
                }

                matrices.pop();
            }

            try {
                entities.render(state.state(), camera, 0, 0, 0, matrices, queue);
            } catch (Throwable t) {
                handleError("player model", t);
            }

            if (state.state.hasVehicle) {
                matrices.push();
                matrices.translate(0, -1, 0);
                try {
                    entities.render(BOAT_STATE, camera, 0, 0, 0, matrices, queue);
                } catch (Throwable t) {
                    handleError("boat", t);
                }
                matrices.pop();
            }

            matrices.pop();

            renderDispatcher.render();
        } catch (Throwable t) {
            handleError("root", t);
        }
    }

    private void handleError(String stage, Throwable t) {
        LOGGER.error("Error occured whilst rendering player preview @" + stage, t);
    }

    @Override
    protected float getYOffset(int height, int windowScaleFactor) {
        return height * 0.5F;
    }

    public record Element(
            PlayerEntityRenderState state, Vector3f translation, Quaternionf rotation,
            ScreenRect bounds, int x1, int x2, int y1, int y2, float scale, @Nullable ScreenRect scissorArea
    ) implements SpecialGuiElementRenderState {
        public Element(PlayerEntityRenderState state, Vector3f translation, Quaternionf rotation, int x1, int x2, int y1, int y2, float scale, @Nullable ScreenRect scissorArea) {
            this(state, translation, rotation, SpecialGuiElementRenderState.createBounds(x1, y1, x2, y2, scissorArea), x1, x2, y1, y2, scale, scissorArea);
        }

        public Element {
            translation = new Vector3f(translation);
            rotation = new Quaternionf(rotation);
        }
    }
}
