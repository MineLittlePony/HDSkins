package com.minelittlepony.hdskins.dummy;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.minelittlepony.hdskins.gui.DummyWorld;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
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
    public boolean hasVehicle() {
        return textures.previewRiding;
    }

    @Override
    public boolean isSleeping() {
        return !textures.previewRiding && textures.previewSleeping;
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
        return !textures.previewRiding && !textures.previewSleeping && super.isSneaking();
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

        handSwingProgress = handSwingTicks / 8F;

        upwardSpeed *= 0.98;
        if (Math.abs(upwardSpeed) < 0.003) {
            upwardSpeed = 0;
        }

        if (y == 0 && jumping && !textures.previewSleeping && !textures.previewRiding) {
            jump();
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
