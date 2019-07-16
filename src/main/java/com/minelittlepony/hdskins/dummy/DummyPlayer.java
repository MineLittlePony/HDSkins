package com.minelittlepony.hdskins.dummy;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AbsoluteHand;
import java.util.Map;

/**
 * A dummy player that appears on the skins gui when previewing a skin.
 */
@SuppressWarnings("EntityConstructor")
public class DummyPlayer extends LivingEntity {

    private final Map<EquipmentSlot, ItemStack> armour = Maps.newEnumMap(ImmutableMap.of(
            EquipmentSlot.HEAD, ItemStack.EMPTY,
            EquipmentSlot.CHEST, ItemStack.EMPTY,
            EquipmentSlot.LEGS, ItemStack.EMPTY,
            EquipmentSlot.FEET, ItemStack.EMPTY,
            EquipmentSlot.MAINHAND, ItemStack.EMPTY
    ));

    private final TextureProxy textures;

    public DummyPlayer(TextureProxy textures) {
        super(EntityType.PLAYER, DummyWorld.INSTANCE);

        this.textures = textures;
    }

    public TextureProxy getTextures() {
        return textures;
    }

    @Override
    public double getHeightOffset() {
        return -0.35D;
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
            return EntityPose.SNEAKING;
        }
        return EntityPose.STANDING;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public float method_6024(float float_1) {
       return isSwimming() ? 1 : 0;
    }

    @Override
    public boolean isInWater() {
        return isSwimming();
    }

    @Override
    public boolean isSwimming() {
        return textures.previewSwimming;
    }

    @Override
    public boolean isInsideWater() {
        return isSwimming();
    }

    @Override
    public boolean canSeePlayer(PlayerEntity player) {
        return false; // we're not in game. The player might be null and we don't really care about name plates anyway.
    }

    @Override
    public Entity getPrimaryPassenger() {
        return textures.previewRiding ? RenderDummyPlayer.MrBoaty.instance : null;
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

        age++;
    }

    @Override
    public AbsoluteHand getMainHand() {
        return MinecraftClient.getInstance().options.mainHand;
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return armour.values();
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slotIn) {
        return armour.get(slotIn);
    }

    @Override
    public void setEquippedStack(EquipmentSlot slotIn, ItemStack stack) {
        armour.put(slotIn, stack);
    }
}
