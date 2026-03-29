package com.minelittlepony.hdskins.client.gui;

import java.util.Optional;

import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.common.util.registry.ForwardingHolder;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;

public class PlayerBodyWidget<S extends AvatarRenderState> implements Carousel.Element {
    private static final Optional<ItemStackTemplate> ELYTRA = Optional.of(new ItemStackTemplate(Items.ELYTRA, DataComponentPatch.builder()
            .set(DataComponents.GLIDER, Unit.INSTANCE)
            .set(
                DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.CHEST).setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA).setAsset(EquipmentAssets.ELYTRA).build()
            )
            .build()));

    protected final Vector3f position = new Vector3f();

    private final Vector3d offset = new Vector3d();
    private final Vector3f velocity = new Vector3f();
    private final WalkAnimationState limbAnimator = new WalkAnimationState();
    private final ElytraState elytraState = new ElytraState();

    public boolean sprinting;
    public boolean jumping;

    public PlayerSkins<?> skins;
    public final S playerState;

    public float lastHandSwingProgress;
    public float nextHandSwingProgress;
    public int handSwingTicks;
    public float upwardSpeed;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public PlayerBodyWidget(PlayerSkins<?> skins, S playerState) {
        this.skins = skins;
        this.playerState = playerState;
        this.playerState.entityType = EntityType.PLAYER;
        this.playerState.mainArm = Minecraft.getInstance().options.mainHand().get();

        if (Minecraft.getInstance().player != null) {
            try {
                ((EntityRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(playerState))
                    .extractRenderState(Minecraft.getInstance().player, playerState, 1);
            } catch (Throwable ignored) {}
        }
        this.playerState.y = 14;
    }

    public void setPose(Pose pose) {
        playerState.pose = pose == Pose.STANDING && playerState.isCrouching ? Pose.CROUCHING : pose;
    }

    public void swingArm(InteractionHand hand) {
        playerState.isUsingItem = true;
        playerState.useItemHand = hand;
        playerState.attackArm = hand == InteractionHand.MAIN_HAND ? playerState.mainArm : playerState.mainArm.getOpposite();
    }

    public void setHandStack(InteractionHand hand, Optional<ItemStackTemplate> stack) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? playerState.mainArm : playerState.mainArm.getOpposite();
        Minecraft.getInstance().getItemModelResolver().appendItemLayers(
                arm == HumanoidArm.LEFT ? playerState.leftHandItemState : playerState.rightHandItemState,
                createStack(stack),
                arm == HumanoidArm.LEFT ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                null,
                null,
                0
        );
    }

    public void setEquippedStack(EquipmentSlot slot, Optional<ItemStackTemplate> stack) {
        switch (slot) {
            case HEAD:
                playerState.headEquipment = createStack(stack);
                break;
            case SADDLE:
            case BODY:
            case CHEST:
                playerState.chestEquipment = createStack(stack);
                break;
            case FEET:
                playerState.feetEquipment = createStack(stack);
                break;
            case LEGS:
                playerState.legsEquipment = createStack(stack);
                break;
            case MAINHAND:
                setHandStack(InteractionHand.MAIN_HAND, stack);
                break;
            case OFFHAND:
                setHandStack(InteractionHand.OFF_HAND, stack);
                break;
        }
    }

    private ItemStack createStack(Optional<ItemStackTemplate> template) {
        return template.map(t -> t.typeHolder().areComponentsBound() ? t.create() : new ItemStack(ForwardingHolder.withComponents(t.typeHolder(), DataComponentMap.builder()
                    .addAll(DataComponents.COMMON_ITEM_COMPONENTS)
                    .set(DataComponents.ITEM_MODEL, t.item().unwrapKey().orElseThrow().identifier())
                .build()), 1, t.components())).orElse(ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        SkinType type = skins.getPosture().getActiveSkinType();

        playerState.skin = skins.getSkinTextureBundle();

        if ((type == SkinType.ELYTRA) != (playerState.chestEquipment.getItem() == Items.ELYTRA)) {
            setEquippedStack(EquipmentSlot.CHEST, (type == SkinType.ELYTRA ? ELYTRA : skins.getPosture().getEquipment().getStack(EquipmentSlot.CHEST)));
        }

        lastHandSwingProgress = playerState.ticksUsingItem;

        if (playerState.isUsingItem) {
            if (++handSwingTicks >= 8) {
                handSwingTicks = 0;
                playerState.isUsingItem = false;
            }
        } else {
            handSwingTicks = 0;
        }

        playerState.pose = skins.getPosture().getPose().getPose();
        playerState.isPassenger = playerState.pose == Pose.SITTING;

        nextHandSwingProgress = handSwingTicks / 8F;

        upwardSpeed -= 0.1F;
        if (Math.abs(upwardSpeed) < 0.003) {
            upwardSpeed = 0;
        }

        if (playerState.y == 0 && jumping && upwardSpeed <= 0 && !playerState.hasPose(Pose.SLEEPING) && !playerState.isPassenger && !playerState.isVisuallySwimming && !playerState.isAutoSpinAttack) {
            upwardSpeed = velocity.y;
        }

        if (jumping && playerState.y <= 0) {
            jumping = false;
            upwardSpeed += 4;
        }

        upwardSpeed *= 0.988;
        upwardSpeed -= 0.58D;

        playerState.y += upwardSpeed;

        if (playerState.y < 0) {
            playerState.y = 0;
        }

        playerState.isVisuallySwimming = playerState.hasPose(Pose.SWIMMING);
        playerState.isAutoSpinAttack = playerState.hasPose(Pose.SPIN_ATTACK);

        limbAnimator.update(sprinting ? (playerState.isCrouching ? 0.1F : 1) : (playerState.isVisuallySwimming ? 1 : 0), 0.1F, 1);
        elytraState.update(playerState);

        playerState.ageInTicks++;

        offset.set(0, -1.25 + playerState.y / 16F, 0);

        if (playerState.isCrouching) {
            offset.y += 0.125D;
        }

        if (playerState.hasPose(Pose.SLEEPING)) {
            offset.y += 0.7F;
            offset.x++;
        }
        if (playerState.isVisuallySwimming) {
            playerState.flyingYRot = 0.7F;
            if (velocity.x < 100) {
                velocity.x += 100;
            }

            offset.y += 0.5F;
        } else if (playerState.isAutoSpinAttack) {
            playerState.flyingYRot = 0;
            offset.y += 1;
            offset.z -= 0.5F;
        } else {
            playerState.flyingYRot = 0;
            if (velocity.x >= 100) {
                velocity.x -= 100;
            }
        }

        playerState.ageInTicks += 0.5F;
        playerState.passengerOffset = new Vec3(offset.x, offset.y, offset.z);
        playerState.lightCoords = LightCoordsUtil.pack(0, Math.min((int)playerState.ageInTicks, 15));
    }

    @Override
    public void updateState(float xPosition, float yPosition, float mouseX, float mouseY, float tickDelta) {
        playerState.bodyRot = 0;
        playerState.yRot = Mth.wrapDegrees(playerState.bodyRot - ((float)Math.atan((xPosition - mouseX) / 20) * -30) * ((float)Math.sin((playerState.bodyRot * (Math.PI / 180)) + 45)));

        playerState.xRot = playerState.hasPose(Pose.SLEEPING) ? 10 : (float)Math.atan(mouseY / 40) * -20;
        playerState.elytraRotX = elytraState.leftWingPitch(tickDelta);
        playerState.elytraRotY = elytraState.leftWingYaw(tickDelta);
        playerState.elytraRotZ = elytraState.leftWingRoll(tickDelta);
        playerState.walkAnimationPos = Mth.lerp(tickDelta, lastHandSwingProgress, nextHandSwingProgress);
        playerState.walkAnimationSpeed = 1;
        if (!playerState.isPassenger) {
            playerState.walkAnimationPos = limbAnimator.position(tickDelta);
            playerState.walkAnimationSpeed = limbAnimator.speed(tickDelta);
        } else {
            playerState.walkAnimationPos = 0;
            playerState.walkAnimationSpeed = 0;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, Bounds bounds, int mouseX, int mouseY, Quaternionf rotation) {
        context.guiRenderState.addPicturesInPictureState(new PlayerPreviewSpecialGuiElementRenderer.Element(
                playerState,
                position,
                rotation,
                bounds.left, bounds.right(), bounds.top, bounds.bottom(),
                bounds.height / 3F,
                context.scissorStack.peek()
        ));
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

        public void update(AvatarRenderState state) {
            lastLeftWingPitch = leftWingPitch;
            lastLeftWingYaw = leftWingYaw;
            lastLeftWingRoll = leftWingRoll;
            float g = state.isCrouching ? Mth.HALF_PI / 9F : STANDING_PITCH;
            float h = state.isCrouching ? -Mth.PI / 4F : STANDING_ROLL;
            float i = state.isCrouching ? 0.08726646F : 0;

            leftWingPitch += (g - leftWingPitch) * 0.3F;
            leftWingYaw += (i - leftWingYaw) * 0.3F;
            leftWingRoll += (h - leftWingRoll) * 0.3F;
        }

        public float leftWingPitch(float tickProgress) {
            return Mth.lerp(tickProgress, lastLeftWingPitch, leftWingPitch);
        }

        public float leftWingYaw(float tickProgress) {
            return Mth.lerp(tickProgress, lastLeftWingYaw, leftWingYaw);
        }

        public float leftWingRoll(float tickProgress) {
            return Mth.lerp(tickProgress, lastLeftWingRoll, leftWingRoll);
        }
    }
}
