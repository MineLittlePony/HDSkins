package com.minelittlepony.hdskins.gui;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.minelittlepony.hdskins.SkinUploader;
import com.minelittlepony.hdskins.resources.LocalTexture;
import com.minelittlepony.hdskins.resources.LocalTexture.IBlankSkinSupplier;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinProvider.SkinTextureAvailableCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AbsoluteHand;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("EntityConstructor")
public class EntityPlayerModel extends LivingEntity implements IBlankSkinSupplier {

    public static final Identifier NO_SKIN = new Identifier("hdskins", "textures/mob/noskin.png");
    public static final Identifier NO_ELYTRA = new Identifier("textures/entity/elytra.png");

    private final Map<EquipmentSlot, ItemStack> armour = Maps.newEnumMap(ImmutableMap.of(
            EquipmentSlot.HEAD, ItemStack.EMPTY,
            EquipmentSlot.CHEST, ItemStack.EMPTY,
            EquipmentSlot.LEGS, ItemStack.EMPTY,
            EquipmentSlot.FEET, ItemStack.EMPTY,
            EquipmentSlot.MAINHAND, ItemStack.EMPTY
    ));

    protected final LocalTexture skin;
    protected final LocalTexture elytra;

    private final GameProfile profile;

    protected boolean previewThinArms = false;
    protected boolean previewSleeping = false;
    protected boolean previewRiding = false;

    public EntityPlayerModel(GameProfile gameprofile) {
        super(EntityType.PLAYER, DummyWorld.INSTANCE);

        profile = gameprofile;

        skin = new LocalTexture(profile, Type.SKIN, this);
        elytra = new LocalTexture(profile, Type.ELYTRA, this);
    }

    public CompletableFuture<Void> reloadRemoteSkin(SkinUploader uploader, SkinTextureAvailableCallback listener) {
        return uploader.loadTextures(profile).thenAcceptAsync(ptm -> {
            skin.setRemote(ptm, listener);
            elytra.setRemote(ptm, listener);
        }, MinecraftClient.getInstance()::execute); // run on main thread
    }

    public void setLocalTexture(Path skinTextureFile, Type type) {
        if (type == Type.SKIN) {
            skin.setLocal(skinTextureFile);
        } else if (type == Type.ELYTRA) {
            elytra.setLocal(skinTextureFile);
        }
    }

    @Override
    public Identifier getBlankSkin(Type type) {
        return type == Type.SKIN ? NO_SKIN : NO_ELYTRA;
    }

    public boolean isUsingLocalTexture() {
        return skin.usingLocal() || elytra.usingLocal();
    }

    public boolean isTextureSetupComplete() {
        return skin.uploadComplete() && elytra.uploadComplete();
    }

    public boolean isUsingRemoteTexture() {
        return skin.hasRemoteTexture() || elytra.hasRemoteTexture();
    }

    public void releaseTextures() {
        skin.clearLocal();
        elytra.clearLocal();
    }

    public LocalTexture getLocal(Type type) {
        return type == Type.SKIN ? skin : elytra;
    }

    public void setPreviewThinArms(boolean thinArms) {
        previewThinArms = thinArms;
    }

    public boolean usesThinSkin() {
        if (skin.uploadComplete() && skin.getRemote().hasModel()) {
            return skin.getRemote().usesThinArms();
        }

        return previewThinArms;
    }

    public void setSleeping(boolean sleep) {
        previewSleeping = sleep;
    }

    public void setRiding(boolean ride) {
        previewRiding = ride;
    }

    @Override
    public boolean hasVehicle() {
        return previewRiding;
    }

    @Override
    public boolean isSleeping() {
        return !previewRiding && previewSleeping;
    }

    @Override
    public Entity getPrimaryPassenger() {
        return previewRiding ? RenderPlayerModel.MrBoaty.instance : null;
    }

    @Override
    public boolean isSneaking() {
        return !previewRiding && !previewSleeping && super.isSneaking();
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

        if (y == 0 && jumping && !previewSleeping && !previewRiding) {
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
