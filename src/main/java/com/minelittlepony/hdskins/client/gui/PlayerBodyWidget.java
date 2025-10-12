package com.minelittlepony.hdskins.client.gui;

import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.state.BoatEntityRenderState;
import net.minecraft.client.render.entity.state.FallingBlockEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LimbAnimator;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PlayerBodyWidget<S extends PlayerEntityRenderState> implements Carousel.Element {
    private static final BoatEntityRenderState BOAT_STATE = new BoatEntityRenderState() {{
        entityType = EntityType.OAK_BOAT;
        light = LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE;
    }};
    private static final FallingBlockEntityRenderState BED_HEAD_STATE = new FallingBlockEntityRenderState() {{
        entityType = EntityType.FALLING_BLOCK;
        light = LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE;
        movingBlockRenderState.blockState = Blocks.RED_BED.getDefaultState().with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, Direction.SOUTH);
    }};
    private static final FallingBlockEntityRenderState BED_FOOT_STATE = new FallingBlockEntityRenderState() {{
        entityType = EntityType.FALLING_BLOCK;
        light = LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE;
        movingBlockRenderState.blockState = Blocks.RED_BED.getDefaultState().with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, Direction.SOUTH);
        movingBlockRenderState.entityBlockPos = movingBlockRenderState.entityBlockPos.offset(Direction.SOUTH);
        movingBlockRenderState.fallingBlockPos = movingBlockRenderState.entityBlockPos;
    }};

    protected final Vector3f position = new Vector3f();

    private final Vector3d offset = new Vector3d();
    private final Vector3f velocity = new Vector3f();
    private final LimbAnimator limbAnimator = new LimbAnimator();
    private final ElytraState elytraState = new ElytraState();

    public boolean sprinting;
    public boolean jumping;

    public PlayerSkins<?> skins;
    public final S playerState;

    public float lastHandSwingProgress;
    public float nextHandSwingProgress;
    public int handSwingTicks;
    public float upwardSpeed;

    public PlayerBodyWidget(PlayerSkins<?> skins, S playerState) {
        this.skins = skins;
        this.playerState = playerState;
        this.playerState.entityType = EntityType.PLAYER;
        this.playerState.mainArm = MinecraftClient.getInstance().options.getMainArm().getValue();
    }

    public void setPose(EntityPose pose) {
        playerState.pose = pose == EntityPose.STANDING && playerState.isInSneakingPose ? EntityPose.CROUCHING : pose;
    }

    public void swingArm(Hand hand) {
        playerState.isUsingItem = true;
        playerState.activeHand = hand;
        playerState.preferredArm = hand == Hand.MAIN_HAND ? playerState.mainArm : playerState.mainArm.getOpposite();
    }

    public void setHandStack(Hand hand, ItemStack stack) {
        Arm arm = hand == Hand.MAIN_HAND ? playerState.mainArm : playerState.mainArm.getOpposite();
        MinecraftClient.getInstance().getItemModelManager().clearAndUpdate(
                arm == Arm.LEFT ? playerState.leftHandItemState : playerState.rightHandItemState,
                stack,
                arm == Arm.LEFT ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                null,
                null,
                0
        );
    }

    @Override
    public void tick() {
        SkinType type = skins.getPosture().getActiveSkinType();

        playerState.skinTextures = skins.getSkinTextureBundle();

        if ((type == SkinType.ELYTRA) != (playerState.equippedChestStack.getItem() == Items.ELYTRA)) {
            playerState.equippedChestStack = (type == SkinType.ELYTRA ? Items.ELYTRA.getDefaultStack() : skins.getPosture().getEquipment().getStack(EquipmentSlot.CHEST));
        }

        lastHandSwingProgress = playerState.handSwingProgress;

        if (playerState.isUsingItem) {
            if (++handSwingTicks >= 8) {
                handSwingTicks = 0;
                playerState.isUsingItem = false;
            }
        } else {
            handSwingTicks = 0;
        }

        playerState.pose = skins.getPosture().getPose().getPose();
        playerState.hasVehicle = playerState.pose == EntityPose.SITTING;

        nextHandSwingProgress = handSwingTicks / 8F;

        upwardSpeed -= 0.1F;
        if (Math.abs(upwardSpeed) < 0.003) {
            upwardSpeed = 0;
        }

        if (playerState.y == 0 && jumping && upwardSpeed <= 0 && !playerState.isInPose(EntityPose.SLEEPING) && !playerState.hasVehicle && !playerState.isSwimming && !playerState.usingRiptide) {
            upwardSpeed = velocity.y;
        }

        if (jumping) {
            jumping = false;
            upwardSpeed += 4;
        }

        upwardSpeed *= 0.988;
        upwardSpeed -= 0.58D;

        playerState.y += upwardSpeed;

        if (playerState.y < 0) {
            playerState.y = 0;
        }

        playerState.isSwimming = playerState.isInPose(EntityPose.SWIMMING);
        playerState.usingRiptide = playerState.isInPose(EntityPose.SPIN_ATTACK);

        limbAnimator.updateLimbs(sprinting ? (playerState.isInSneakingPose ? 0.1F : 1) : (playerState.isSwimming ? 1 : 0), 0.1F, 1);
        elytraState.update(playerState);

        playerState.age++;

        offset.set(0, -1.25 + playerState.y / 16F, 0);

        if (playerState.isInSneakingPose) {
            offset.y += 0.125D;
        }

        if (playerState.isInPose(EntityPose.SLEEPING)) {
            offset.y += 0.7F;
            offset.x++;
        }
        if (playerState.isSwimming) {
            playerState.leaningPitch = 0.7F;
            if (velocity.x < 100) {
                velocity.x += 100;
            }

            offset.y += 0.5F;
        } else if (playerState.usingRiptide) {
            playerState.leaningPitch = 0;
            offset.y += 1;
            offset.z -= 0.5F;
        } else {
            playerState.leaningPitch = 0;
            if (velocity.x >= 100) {
                velocity.x -= 100;
            }
        }

        playerState.age += 0.5F;
        playerState.positionOffset = new Vec3d(offset.x, offset.y, offset.z);
        playerState.light = LightmapTextureManager.pack(0, Math.min((int)playerState.age, 15));
    }

    @Override
    public void updateState(float xPosition, float yPosition, float mouseX, float mouseY, float tickDelta) {
        playerState.bodyYaw = 0;
        playerState.relativeHeadYaw = MathHelper.wrapDegrees(playerState.bodyYaw - ((float)Math.atan((xPosition - mouseX) / 20) * -30) * ((float)Math.sin((playerState.bodyYaw * (Math.PI / 180)) + 45)));

        playerState.pitch = playerState.isInPose(EntityPose.SLEEPING) ? 10 : (float)Math.atan(mouseY / 40) * -20;
        playerState.leftWingPitch = elytraState.leftWingPitch(tickDelta);
        playerState.leftWingYaw = elytraState.leftWingYaw(tickDelta);
        playerState.leftWingRoll = elytraState.leftWingRoll(tickDelta);
        playerState.handSwingProgress = MathHelper.lerp(tickDelta, lastHandSwingProgress, nextHandSwingProgress);
        playerState.limbAmplitudeInverse = 1;
        if (!playerState.hasVehicle) {
            playerState.limbSwingAnimationProgress = limbAnimator.getAnimationProgress(tickDelta);
            playerState.limbSwingAmplitude = limbAnimator.getAmplitude(tickDelta);
        } else {
            playerState.limbSwingAnimationProgress = 0;
            playerState.limbSwingAmplitude = 0;
        }
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

    public static class ElytraState {
        private static final float STANDING_PITCH = (float) (Math.PI / 12);
        private static final float STANDING_ROLL = (float) (-Math.PI / 12);
        private float leftWingPitch;
        private float leftWingYaw;
        private float leftWingRoll;
        private float lastLeftWingPitch;
        private float lastLeftWingYaw;
        private float lastLeftWingRoll;

        public void update(PlayerEntityRenderState state) {
            lastLeftWingPitch = leftWingPitch;
            lastLeftWingYaw = leftWingYaw;
            lastLeftWingRoll = leftWingRoll;
            float g = state.isInSneakingPose ? MathHelper.TAU / 9F : STANDING_PITCH;
            float h = state.isInSneakingPose ? -MathHelper.PI / 4F : STANDING_ROLL;
            float i = state.isInSneakingPose ? 0.08726646F : 0;

            leftWingPitch += (g - leftWingPitch) * 0.3F;
            leftWingYaw += (i - leftWingYaw) * 0.3F;
            leftWingRoll += (h - leftWingRoll) * 0.3F;
        }

        public float leftWingPitch(float tickProgress) {
            return MathHelper.lerp(tickProgress, lastLeftWingPitch, leftWingPitch);
        }

        public float leftWingYaw(float tickProgress) {
            return MathHelper.lerp(tickProgress, lastLeftWingYaw, leftWingYaw);
        }

        public float leftWingRoll(float tickProgress) {
            return MathHelper.lerp(tickProgress, lastLeftWingRoll, leftWingRoll);
        }
    }
}
