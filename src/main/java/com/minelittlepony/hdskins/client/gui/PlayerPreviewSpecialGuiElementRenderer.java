package com.minelittlepony.hdskins.client.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;

public class PlayerPreviewSpecialGuiElementRenderer extends PictureInPictureRenderer<PlayerPreviewSpecialGuiElementRenderer.Element> {
    public static final Logger LOGGER = LogManager.getLogger();
    private static final BoatRenderState BOAT_STATE = new BoatRenderState() {{
        entityType = EntityTypes.OAK_BOAT;
        lightCoords = LightCoordsUtil.FULL_BRIGHT;
    }};

    public PlayerPreviewSpecialGuiElementRenderer(PictureInPictureRendererRegistry.Context context) {
    }

    @Override
    protected String getTextureLabel() {
        return "hdskins_player_preview";
    }

    @Override
    public Class<Element> getRenderStateClass() {
        return Element.class;
    }

    @Override
    protected void renderToTexture(Element state, PoseStack stack, SubmitNodeCollector queue) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        CameraRenderState camera = new CameraRenderState();
        EntityRenderDispatcher entities = Minecraft.getInstance().getEntityRenderDispatcher();

        Vector3f pos = state.translation();

        PoseStack matrices = new PoseStack();
        matrices.last().set(stack.last());
        try {
            matrices.pushPose();
            matrices.translate(pos.x, pos.y, pos.z);
            matrices.mulPose(state.rotation());

            if (state.state.hasPose(Pose.SLEEPING)) {
                matrices.pushPose();
                matrices.translate(0, -1.22, -0.5);

                try {
                    var blockState = new MovingBlockRenderState();
                    blockState.blockState = Blocks.BED.red().defaultBlockState().setValue(BedBlock.PART, BedPart.FOOT);
                    queue.submitMovingBlock(stack, blockState, 0);
                } catch (Throwable t) {
                    handleError("bed 1", t);
                }

                matrices.translate(-1, 0, 0);

                try {
                    var blockState = new MovingBlockRenderState();
                    blockState.blockState = Blocks.BED.red().defaultBlockState().setValue(BedBlock.PART, BedPart.HEAD);
                } catch (Throwable t) {
                    handleError("bed 2", t);
                }

                matrices.popPose();
            }

            try {
                entities.submit(state.state(), camera, 0, 0, 0, matrices, queue);
            } catch (Throwable t) {
                handleError("player model", t);
            }

            if (state.state.isPassenger) {
                matrices.pushPose();
                matrices.translate(0, -1, 0);
                try {
                    entities.submit(BOAT_STATE, camera, 0, 0, 0, matrices, queue);
                } catch (Throwable t) {
                    handleError("boat", t);
                }
                matrices.popPose();
            }

            matrices.popPose();
        } catch (Throwable t) {
            handleError("root", t);
        }
    }

    private void handleError(String stage, Throwable t) {
        LOGGER.error("Error occured whilst rendering player preview @" + stage, t);
    }

    @Override
    protected float getTranslateY(int height, int windowScaleFactor) {
        return height * 0.5F;
    }

    public record Element(
            AvatarRenderState state, Vector3f translation, Quaternionf rotation,
            ScreenRectangle bounds, int x0, int x1, int y0, int y1, float scale, @Nullable ScreenRectangle scissorArea
    ) implements PictureInPictureRenderState {
        public Element(AvatarRenderState state, Vector3f translation, Quaternionf rotation, int x1, int x2, int y1, int y2, float scale, @Nullable ScreenRectangle scissorArea) {
            this(state, translation, rotation, PictureInPictureRenderState.getBounds(x1, y1, x2, y2, scissorArea), x1, x2, y1, y2, scale, scissorArea);
        }

        public Element {
            translation = new Vector3f(translation);
            rotation = new Quaternionf(rotation);
        }
    }
}
