package com.minelittlepony.hdskins.client.dummy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;

/**
 * A dummy player that appears on the skins gui when previewing a skin.
 */
@SuppressWarnings("EntityConstructor")
public class DummyPlayer extends OtherClientPlayerEntity {

    private final TextureProxy textures;

    public DummyPlayer(TextureProxy textures) {
        super(new DummyWorld(), textures.getProfile());

        this.textures = textures;
    }

    public TextureProxy getTextures() {
        return textures;
    }

    @Override
    public boolean hasVehicle() {
        return textures.previewRiding;
    }

    @Override
    public boolean isSleeping() {
        return textures.previewSleeping;
    }

    @Override
    public EntityPose getPose() {
        if (isSleeping()) {
            return EntityPose.SLEEPING;
        }
        if (isSwimming()) {
            return EntityPose.SWIMMING;
        }
        if (isSneaking()) {
            return EntityPose.CROUCHING;
        }
        return EntityPose.STANDING;
    }

    @Override
    public float getLeaningPitch(float float_1) {
       return isSwimming() ? 1 : 0;
    }

    @Override
    public boolean isSubmergedInWater() {
        return isSwimming();
    }

    @Override
    public boolean isSwimming() {
        return textures.previewSwimming;
    }

    @Override
    public boolean isTouchingWater() {
        return isSwimming();
    }

    @Override
    public boolean canSeePlayer(PlayerEntity player) {
        return false; // we're not in game. The player might be null and we don't really care about name plates anyway.
    }

    @Override
    public Entity getPrimaryPassenger() {
        return textures.previewRiding ? DummyPlayerRenderer.MrBoaty.instance : null;
    }

    @Override
    public boolean isSneaking() {
        return !textures.previewSwimming && !textures.previewRiding && !textures.previewSleeping && super.isSneaking();
    }

    public void updateModel() {
        lastHandSwingProgress = handSwingProgress;

        if (isHandSwinging) {
            ++handSwingTicks;
            if (handSwingTicks >= 8) {
                handSwingTicks = 0;
                isHandSwinging = false;
            }
        } else {
            handSwingTicks = 0;
        }

        limbAngle = (limbAngle + 1) % 360;

        handSwingProgress = handSwingTicks / 8F;

        upwardSpeed *= 0.98;
        if (Math.abs(upwardSpeed) < 0.003) {
            upwardSpeed = 0;
        }

        double y = getY();

        if (y == 0 && jumping && !textures.previewSleeping && !textures.previewRiding && !textures.previewSwimming) {
            jump();

            upwardSpeed = (float)getVelocity().y;
        }

        upwardSpeed -= 0.08D;
        upwardSpeed *= 0.9800000190734863D;

        y += upwardSpeed;

        if (y < 0) {
            y = 0;
        }
        onGround = y == 0;

        setPos(getX(), y, getZ());

        age++;
    }

    @Override
    public Arm getMainArm() {
        return MinecraftClient.getInstance().options.mainArm;
    }

}
