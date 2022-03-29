package com.minelittlepony.hdskins.client.dummy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.dummy.DummyPlayerRenderer.MrBoaty;
import com.minelittlepony.hdskins.client.resources.LocalTexture;
import com.minelittlepony.hdskins.profile.SkinType;

/**
 * A dummy player that appears on the skins gui when previewing a skin.
 */
@SuppressWarnings("EntityConstructor")
public class DummyPlayer extends AbstractClientPlayerEntity {
    private final Map<EquipmentSlot, ItemStack> armour = new EnumMap<>(EquipmentSlot.class);

    private TextureProxy textures;

    private AttributeContainer attributes;

    public final MrBoaty boat;

    @Deprecated
    public DummyPlayer(TextureProxy textures) {
        this(DummyWorld.getOrDummyFuture().getNow(null), textures);
    }

    public DummyPlayer(ClientWorld world, TextureProxy textures) {
        super(world, MinecraftClient.getInstance().getSession().getProfile());
        refreshPositionAndAngles(0.5D, 0, 0.5D, 0, 0);

        this.textures = textures;
        this.boat = new MrBoaty(world);
    }

    @Override
    public AttributeContainer getAttributes() {
        // initialization order is annoying
        if (attributes == null) {
            attributes = new AttributeContainer(DefaultAttributeRegistry.get(EntityType.PLAYER));
        }
        return this.attributes;
    }

    public TextureProxy getTextures() {
        // initialization order is annoying
        if (textures == null) {
            textures = new TextureProxy(MinecraftClient.getInstance().getSession().getProfile(),
                    type -> PlayerPreview.NO_SKIN_STEVE,
                    type -> PlayerPreview.NO_SKIN_ALEX);
        }
        return textures;
    }

    @Override
    public String getModel() {
        return getTextures().usesThinSkin() ? VanillaModels.SLIM : VanillaModels.DEFAULT;
    }

    @Override
    public boolean canRenderElytraTexture() {
        return getTextures().getSkinType() == SkinType.ELYTRA;
    }

    @Override
    public boolean canRenderCapeTexture() {
        return getTextures().getSkinType() == SkinType.CAPE;
    }

    @Override
    public boolean hasSkinTexture() {
        return true;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return true;
    }

    @Override
    public Identifier getSkinTexture() {
        LocalTexture localTex = getTextures().get(SkinType.SKIN);
        Identifier texture = localTex.getId();
        return texture == null ? localTex.getDefault() : texture;
    }

    @Nullable
    @Override
    public Identifier getCapeTexture() {
        return getTextures().getSkinType() == SkinType.CAPE ? getTextures().get(SkinType.CAPE).getId() : null;
    }

    @Nullable
    @Override
    public Identifier getElytraTexture() {
        return getTextures().getSkinType() == SkinType.ELYTRA ? getTextures().get(SkinType.ELYTRA).getId() : null;
    }

    @Override
    public double getHeightOffset() {
        return -0.35D;
    }

    @Override
    public boolean hasVehicle() {
        return getTextures().previewRiding;
    }

    @Override
    public boolean isSleeping() {
        return getTextures().previewSleeping;
    }

    @Override
    public boolean isSwimming() {
        return getTextures().previewSwimming;
    }

    @Override
    public boolean isUsingRiptide() {
        return getTextures().previewRiptide;
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
    public boolean isTouchingWater() {
        return isSwimming();
    }

    @Override
    public boolean canSee(Entity entity) {
        return false; // we're not in game. The player might be null and we don't really care about name plates anyway.
    }

    @Override
    public Entity getPrimaryPassenger() {
        return hasVehicle() ? boat : null;
    }

    @Override
    public boolean isSneaking() {
        return !isSwimming() && !hasVehicle() && !isSleeping() && super.isSneaking();
    }

    public void updateModel() {

        SkinType type = getTextures().getSkinType();

        if ((type == SkinType.ELYTRA) != (getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA)) {
            equipStack(EquipmentSlot.CHEST, (type == SkinType.ELYTRA ? Items.ELYTRA : Items.AIR).getDefaultStack());
        }

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
        limbDistance = isSprinting() ? (isSneaking() ? 0.1F : 1) : 0;

        handSwingProgress = handSwingTicks / 8F;

        upwardSpeed *= 0.98;
        if (Math.abs(upwardSpeed) < 0.003) {
            upwardSpeed = 0;
        }

        double y = getY();

        if (y == 0 && jumping && !isSleeping() && !hasVehicle() && !isSwimming() && !isUsingRiptide()) {
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
    public boolean isPartVisible(PlayerModelPart part) {
        return MinecraftClient.getInstance().options.isPlayerModelPartEnabled(part);
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
