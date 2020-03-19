package com.minelittlepony.hdskins.client.dummy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;

import java.util.EnumMap;
import java.util.Map;

/**
 * A dummy player that appears on the skins gui when previewing a skin.
 */
@SuppressWarnings("EntityConstructor")
public class DummyPlayer extends LivingEntity {

    public static EntityType<DummyPlayer> TYPE = EntityType.Builder
            .<DummyPlayer>create((t, w) -> new DummyPlayer(t, null), EntityCategory.MISC)
            .disableSaving()
            .disableSummon()
            .build("hdskins:dummy_player");

    private final Map<EquipmentSlot, ItemStack> armour = new EnumMap<>(EquipmentSlot.class);

    private final TextureProxy textures;

    private AttributeContainer attributes;

    @Override
    public AttributeContainer getAttributes() {
        if (attributes == null) {
            attributes = new AttributeContainer(DefaultAttributeRegistry.get(EntityType.PLAYER));
        }
        return this.attributes;
    }

    public DummyPlayer(EntityType<? extends DummyPlayer> type, TextureProxy textures) {
        super(type, DummyWorld.INSTANCE);

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
            return EntityPose.CROUCHING;
        }
        return EntityPose.STANDING;
    }

    @Override
    @Environment(EnvType.CLIENT)
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
    public boolean canSee(Entity entity) {
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

        if (handSwinging) {
            ++handSwingTicks;
            if (handSwingTicks >= 8) {
                handSwingTicks = 0;
                handSwinging = false;
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

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return armour.values();
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return armour.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        armour.put(slot, stack == null ? ItemStack.EMPTY : stack);
    }
}
