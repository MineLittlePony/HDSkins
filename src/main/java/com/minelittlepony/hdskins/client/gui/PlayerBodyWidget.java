package com.minelittlepony.hdskins.client.gui;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.minelittlepony.common.client.gui.dimension.Bounds;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.state.BoatEntityRenderState;
import net.minecraft.client.render.entity.state.FallingBlockEntityRenderState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Direction;

public class PlayerBodyWidget<S extends DummyPlayerRenderState> implements Carousel.Element<S> {
    private static final BoatEntityRenderState BOAT_STATE = new BoatEntityRenderState() {{
        entityType = EntityType.OAK_BOAT;
    }};
    private static final FallingBlockEntityRenderState BED_HEAD_STATE = new FallingBlockEntityRenderState() {{
        entityType = EntityType.FALLING_BLOCK;
        this.blockState = Blocks.RED_BED.getDefaultState().with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, Direction.SOUTH);
    }};
    private static final FallingBlockEntityRenderState BED_FOOT_STATE = new FallingBlockEntityRenderState() {{
        entityType = EntityType.FALLING_BLOCK;
        this.blockState = Blocks.RED_BED.getDefaultState().with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, Direction.SOUTH);
        this.currentPos = this.currentPos.offset(Direction.SOUTH);
        this.fallingBlockPos = this.currentPos;
    }};

    protected final Vector3f position = new Vector3f();

    public final S playerState;

    public PlayerBodyWidget(S playerState) {
        this.playerState = playerState;
    }

    @Override
    public void tick() {
        playerState.tickAnimations();
    }

    @Override
    public void updateState(float xPosition, float yPosition, float mouseX, float mouseY, float tickDelta) {
        playerState.updateState(xPosition, yPosition, mouseX, mouseY, tickDelta);
    }

    @Override
    public void render(DrawContext context, Bounds bounds, int mouseX, int mouseY, Quaternionf rotation) {
        mouseY = bounds.top + bounds.height / 2 - mouseY;
        float scale = bounds.height / 3F;

        if (playerState.isInPose(EntityPose.SLEEPING)) {
            context.addEntity(BED_HEAD_STATE, scale, position.add(0, 0, 0, new Vector3f()), rotation, null, bounds.left, bounds.top, bounds.right(), bounds.bottom());
            context.addEntity(BED_FOOT_STATE, scale, position, rotation, null, bounds.left, bounds.top, bounds.right(), bounds.bottom());
        }

        context.addEntity(playerState, scale, position, rotation, null, bounds.left, bounds.top, bounds.right(), bounds.bottom());
        if (playerState.hasVehicle) {
            context.addEntity(BOAT_STATE, scale, position.add(0, 1, 0, new Vector3f()), rotation, null, bounds.left, bounds.top, bounds.right(), bounds.bottom());
        }
    }
}
