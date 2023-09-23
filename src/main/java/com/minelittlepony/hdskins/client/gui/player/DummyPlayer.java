package com.minelittlepony.hdskins.client.gui.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.SkinTextures;
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
import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.gui.player.DummyPlayerRenderer.MrBoaty;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins.Posture.Pose;
import com.minelittlepony.hdskins.profile.SkinType;

/**
 * A dummy player that appears on the skins gui when previewing a skin.
 */
@SuppressWarnings("EntityConstructor")
public class DummyPlayer extends AbstractClientPlayerEntity {
    private final Map<EquipmentSlot, ItemStack> armour = new EnumMap<>(EquipmentSlot.class);

    private PlayerSkins<?> textures;
    @Nullable
    private PlayerSkins<?> overrideTextures;

    private AttributeContainer attributes;

    public final MrBoaty boat;

    @Deprecated
    public DummyPlayer(PlayerSkins<?> textures) {
        this(DummyWorld.getOrDummyFuture().getNow(null), textures);
    }

    public DummyPlayer(ClientWorld world, PlayerSkins<?> textures) {
        super(world, MinecraftClient.getInstance().getGameProfile());
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

    public PlayerSkins<?> getTextures() {
        // initialization order is annoying
        if (overrideTextures != null) {
            return overrideTextures;
        }

        if (textures == null) {
            return PlayerSkins.EMPTY;
        }
        return textures;
    }

    public void setOverrideTextures(PlayerSkins<?> textures) {
        this.overrideTextures = textures == PlayerSkins.EMPTY ? null : textures;
    }

    @Override
    public SkinTextures method_52814() {
        return getTextures().getSkinTextureBundle();
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
    public boolean hasVehicle() {
        return getTextures().getPosture().getPose() == Pose.RIDE;
    }

    @Override
    public boolean isSleeping() {
        return getTextures().getPosture().getPose() == Pose.SLEEP;
    }

    @Override
    public boolean isSwimming() {
        return getTextures().getPosture().getPose() == Pose.SWIM;
    }

    @Override
    public boolean isUsingRiptide() {
        return getTextures().getPosture().getPose() == Pose.RIPTIDE;
    }

    @Override
    public EntityPose getPose() {
        if (getTextures().getPosture().getPose() == Pose.STAND && isSneaking()) {
            return EntityPose.CROUCHING;
        }
        return getTextures().getPosture().getPose().getPose();
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
    public Entity getVehicle() {
        return hasVehicle() ? boat : null;
    }

    @Override
    public boolean isSneaking() {
        return !isSwimming() && !hasVehicle() && !isSleeping() && super.isSneaking();
    }

    public void updateModel() {
        SkinType type = getTextures().getPosture().getActiveSkinType();

        if ((type == SkinType.ELYTRA) != (getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA)) {
            equipStack(EquipmentSlot.CHEST, (type == SkinType.ELYTRA ? Items.ELYTRA.getDefaultStack() : getTextures().getPosture().getEquipment().getStack(EquipmentSlot.CHEST)));
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

        limbAnimator.updateLimbs(isSprinting() ? (isSneaking() ? 0.1F : 1) : (isSwimming() ? 1 : 0), 0.1F);

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
        setOnGround(y == 0);

        setPos(getX(), y, getZ());

        age++;
    }

    @Override
    public Arm getMainArm() {
        return MinecraftClient.getInstance().options.getMainArm().getValue();
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
