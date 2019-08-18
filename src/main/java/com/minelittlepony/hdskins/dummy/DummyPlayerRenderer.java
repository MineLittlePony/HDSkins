package com.minelittlepony.hdskins.dummy;

import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.feature.ArmorBipedFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import org.lwjgl.opengl.GL11;

import com.minelittlepony.common.client.gui.OutsideWorldRenderer;
import com.minelittlepony.hdskins.profile.SkinType;
import java.util.Set;

import static com.mojang.blaze3d.platform.GlStateManager.*;

public class DummyPlayerRenderer<T extends DummyPlayer, M extends PlayerEntityModel<T>> extends LivingEntityRenderer<T, M> {

    /**
     * The basic Elytra texture.
     */
    protected final Identifier TEXTURE_ELYTRA = new Identifier("textures/entity/elytra.png");

    private static final PlayerEntityModel<DummyPlayer> FAT = new PlayerEntityModel<>(0, false);
    private static final PlayerEntityModel<DummyPlayer> THIN = new PlayerEntityModel<>(0, true);

    @SuppressWarnings("unchecked")
    public DummyPlayerRenderer(EntityRenderDispatcher renderer, EntityRendererRegistry.Context context) {
        super(renderer, (M)FAT, 0);
        addFeature(getElytraLayer());
        addFeature(getArmourLayer());
        addFeature(getHeldItemLayer());
    }

    protected FeatureRenderer<T, M> getArmourLayer() {
        return new ArmorBipedFeatureRenderer<>(this,
                new BipedEntityModel<>(0.5F),
                new BipedEntityModel<>(1F)
        );
    }

    protected FeatureRenderer<T, M> getHeldItemLayer() {
        return new HeldItemFeatureRenderer<>(this);
    }

    protected FeatureRenderer<T, M> getElytraLayer() {
        return new DummyPlayerElytraLayer<>(this);
    }

    @Override
    protected Identifier getTexture(T entity) {
        return entity.getTextures().get(SkinType.SKIN).getId();
    }

    @Override
    protected boolean hasLabel(T entity) {
        return MinecraftClient.getInstance().player != null && super.hasLabel(entity);
    }

    @SuppressWarnings("unchecked")
    public M getEntityModel(T entity) {
        return (M)(entity.getTextures().usesThinSkin() ? THIN : FAT);
    }

    @Override
    protected boolean method_4055(T entity) {
        return MinecraftClient.getInstance().player != null && super.method_4055(entity);
    }

    @Override
    public void render(T entity, double x, double y, double z, float entityYaw, float partialTicks) {

        if (entity.isSleeping()) {
            BedHead.instance.render(entity);
        }

        if (entity.hasVehicle()) {
            MrBoaty.instance.render();
        }

        model = getEntityModel(entity);

        Set<PlayerModelPart> parts = MinecraftClient.getInstance().options.getEnabledPlayerModelParts();
        model.headwear.field_3664 = !parts.contains(PlayerModelPart.HAT);
        model.bodyOverlay.field_3664 = !parts.contains(PlayerModelPart.JACKET);
        model.leftLegOverlay.field_3664 = !parts.contains(PlayerModelPart.LEFT_PANTS_LEG);
        model.rightLegOverlay.field_3664 = !parts.contains(PlayerModelPart.RIGHT_PANTS_LEG);
        model.leftArmOverlay.field_3664 = !parts.contains(PlayerModelPart.LEFT_SLEEVE);
        model.rightArmOverlay.field_3664 = !parts.contains(PlayerModelPart.RIGHT_SLEEVE);
        model.isSneaking = entity.isSneaking();

        model.leftArmPose = ArmPose.EMPTY;
        model.rightArmPose = ArmPose.EMPTY;

        double offset = entity.y;

        if (entity.hasVehicle()) {
            offset = entity.getMountedHeightOffset() - entity.getHeight();
        }

        if (model.isSneaking) {
            y -= 0.125D;
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        pushMatrix();
        scalef(1, -1, 1);
        translated(0.001, offset, 0.001);

        if (entity.isSleeping()) {
            rotatef(-90, 0, 1, 0);

            y += 0.7F;
            x += 1;
        }
        if (entity.isSwimming()) {
            DummyWorld.INSTANCE.fillWith(Blocks.WATER.getDefaultState());
            rotatef(45, 1, 0, 0);

            y -= 0.5F;
            x -= 0;
            z -= 1;
        } else {
            DummyWorld.INSTANCE.fillWith(Blocks.AIR.getDefaultState());
        }

        super.render(entity, x, y, z, entityYaw, partialTicks);
        popMatrix();
        GL11.glPopAttrib();
    }

    static class BedHead extends BedBlockEntity {
        public static BedHead instance = new BedHead();

        public BedHead() {
            this.setColor(DyeColor.RED);
        }

        public void render(Entity entity) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            pushMatrix();

            scalef(-1, -1, -1);

            OutsideWorldRenderer.configure(entity.getEntityWorld())
                .get(this)
                .render(this, -0.5F, 0, 0, 0, -1);

            popMatrix();
            GL11.glPopAttrib();
        }
    }

    public static class MrBoaty extends BoatEntity {
        public static MrBoaty instance = new MrBoaty();

        public MrBoaty() {
            super(EntityType.BOAT, DummyWorld.INSTANCE);
        }

        public void render() {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            pushMatrix();

            scalef(-1, -1, -1);

            EntityRenderer<BoatEntity> render = MinecraftClient.getInstance().getEntityRenderManager().getRenderer(this);

            render.render(this, 0, 0, 0, 0, 0);

            popMatrix();
            GL11.glPopAttrib();
        }
    }
}
