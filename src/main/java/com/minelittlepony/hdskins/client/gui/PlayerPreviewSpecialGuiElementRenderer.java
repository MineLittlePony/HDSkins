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
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BedRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BedPart;

public class PlayerPreviewSpecialGuiElementRenderer extends PictureInPictureRenderer<PlayerPreviewSpecialGuiElementRenderer.Element> {
    public static final Logger LOGGER = LogManager.getLogger();
    private static final BoatRenderState BOAT_STATE = new BoatRenderState() {{
        entityType = EntityType.OAK_BOAT;
        lightCoords = LightCoordsUtil.FULL_BRIGHT;
    }};
    private static final BedRenderState BED_STATE = new BedRenderState() {{
        blockEntityType = BlockEntityType.BED;
        color = DyeColor.RED;
        facing = Direction.WEST;
    }};

    public PlayerPreviewSpecialGuiElementRenderer(PictureInPictureRendererRegistry.Context context) {
        super(context.bufferSource());
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
    protected void renderToTexture(Element state, PoseStack stack) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        FeatureRenderDispatcher renderDispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
        CameraRenderState camera = new CameraRenderState();
        BlockEntityRenderDispatcher blocks = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        EntityRenderDispatcher entities = Minecraft.getInstance().getEntityRenderDispatcher();
        SubmitNodeStorage queue = renderDispatcher.getSubmitNodeStorage();

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
                BED_STATE.part = BedPart.FOOT;
                BED_STATE.lightCoords = LightCoordsUtil.FULL_BRIGHT;
                BED_STATE.color = DyeColor.RED;

                try {
                    blocks.submit(BED_STATE, matrices, queue, camera);
                } catch (Throwable t) {
                    handleError("bed 1", t);
                }

                BED_STATE.part = BedPart.HEAD;
                matrices.translate(-1, 0, 0);

                try {
                    blocks.submit(BED_STATE, matrices, queue, camera);
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

            renderDispatcher.renderAllFeatures();
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
