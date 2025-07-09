package com.minelittlepony.hdskins.client.gui;

import org.joml.Vector3d;
import org.joml.Vector3f;

import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LimbAnimator;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class DummyPlayerRenderState extends PlayerEntityRenderState {
    public PlayerSkins<?> skins;

    private float lastHandSwingProgress;
    private float nextHandSwingProgress;
    private int handSwingTicks;
    private float upwardSpeed;

    public boolean sprinting;
    public boolean jumping;

    private final Vector3d offset = new Vector3d();
    public final Vector3f velocity = new Vector3f();
    public final LimbAnimator limbAnimator = new LimbAnimator();
    public final ElytraState elytraState = new ElytraState();

    public DummyPlayerRenderState(PlayerSkins<?> skins) {
        this.skins = skins;
        this.entityType = EntityType.PLAYER;
    }

    public void setPose(EntityPose pose) {
        this.pose = pose == EntityPose.STANDING && this.isInSneakingPose ? EntityPose.CROUCHING : pose;
    }

    public void tickAnimations() {
        SkinType type = skins.getPosture().getActiveSkinType();

        skinTextures = skins.getSkinTextureBundle();

        if ((type == SkinType.ELYTRA) != (equippedChestStack.getItem() == Items.ELYTRA)) {
            equippedChestStack = (type == SkinType.ELYTRA ? Items.ELYTRA.getDefaultStack() : skins.getPosture().getEquipment().getStack(EquipmentSlot.CHEST));
        }

        lastHandSwingProgress = handSwingProgress;

        if (handSwinging) {
            if (++handSwingTicks >= 8) {
                handSwingTicks = 0;
                handSwinging = false;
            }
        } else {
            handSwingTicks = 0;
        }

        pose = skins.getPosture().getPose().getPose();
        hasVehicle = pose == EntityPose.SITTING;



        limbAnimator.updateLimbs(sprinting ? (isInPose(EntityPose.CROUCHING) ? 0.1F : 1) : (isInPose(EntityPose.SWIMMING) ? 1 : 0), 0.1F, 1);

        nextHandSwingProgress = handSwingTicks / 8F;

        upwardSpeed *= 0.98;
        if (Math.abs(upwardSpeed) < 0.003) {
            upwardSpeed = 0;
        }

        if (y == 0 && jumping && !isInPose(EntityPose.SLEEPING) && !hasVehicle && !isSwimming && !usingRiptide) {
            upwardSpeed = velocity.y;
        }

        if (jumping) {
            upwardSpeed += 10;
        }

        upwardSpeed -= 0.08D;
        upwardSpeed *= 0.9800000190734863D;

        y += upwardSpeed;

        if (y < 0) {
            y = 0;
        }

        isSwimming = isInPose(EntityPose.SWIMMING);
        usingRiptide = isInPose(EntityPose.SPIN_ATTACK);

        elytraState.update();

        age++;

        offset.set(0, -1.25, 0);

        if (isInSneakingPose) {
            offset.y += 0.125D;
        }

        if (isInPose(EntityPose.SLEEPING)) {
            offset.y += 0.7F;
            offset.x++;
        }
        if (isSwimming) {
            leaningPitch = 0.7F;
            if (velocity.x < 100) {
                velocity.x += 100;
            }

            offset.y += 0.5F;
        } else if (usingRiptide) {
            offset.y += 1;
            offset.z -= 0.5F;
        } else {
            leaningPitch = 0;
            if (velocity.x >= 100) {
                velocity.x -= 100;
            }
        }

        positionOffset = new Vec3d(offset.x, offset.y, offset.z);
    }

    public void updateState(float xPosition, float yPosition, float mouseX, float mouseY, float tickDelta) {
        bodyYaw = 0;
        relativeHeadYaw = MathHelper.wrapDegrees(bodyYaw - ((float)Math.atan((xPosition - mouseX) / 20) * -30) * ((float)Math.sin((bodyYaw * (Math.PI / 180)) + 45)));

        pitch = isInPose(EntityPose.SLEEPING) ? 10 : (float)Math.atan(mouseY / 40) * -20;
        leftWingPitch = elytraState.leftWingPitch(tickDelta);
        leftWingYaw = elytraState.leftWingYaw(tickDelta);
        leftWingRoll = elytraState.leftWingRoll(tickDelta);
        handSwingProgress = MathHelper.lerp(tickDelta, lastHandSwingProgress, nextHandSwingProgress);
        limbAmplitudeInverse = 1.0F;
        if (!hasVehicle) {
            limbSwingAnimationProgress = limbAnimator.getAnimationProgress(tickDelta);
            limbSwingAmplitude = limbAnimator.getAmplitude(tickDelta);
        } else {
            limbSwingAnimationProgress = 0;
            limbSwingAmplitude = 0;
        }

        if (limbAmplitudeInverse < 1) {
            limbAmplitudeInverse = 1;
        }
    }

    public class ElytraState {
        private static final float STANDING_PITCH = (float) (Math.PI / 12);
        private static final float STANDING_ROLL = (float) (-Math.PI / 12);
        private float leftWingPitch;
        private float leftWingYaw;
        private float leftWingRoll;
        private float lastLeftWingPitch;
        private float lastLeftWingYaw;
        private float lastLeftWingRoll;

        public void update() {
            lastLeftWingPitch = leftWingPitch;
            lastLeftWingYaw = leftWingYaw;
            lastLeftWingRoll = leftWingRoll;
            float g = isInSneakingPose ? MathHelper.TAU / 9F : STANDING_PITCH;
            float h = isInSneakingPose ? -MathHelper.PI / 4F : STANDING_ROLL;
            float i = isInSneakingPose ? 0.08726646F : 0;

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
