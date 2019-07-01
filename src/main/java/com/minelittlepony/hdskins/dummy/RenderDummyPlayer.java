package com.minelittlepony.hdskins.dummy;

import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Items;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import org.lwjgl.opengl.GL11;

import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import java.util.Set;

import static com.mojang.blaze3d.platform.GlStateManager.*;

public class RenderDummyPlayer<T extends DummyPlayer, M extends PlayerEntityModel<T>> extends LivingEntityRenderer<T, M> {

    /**
     * The basic Elytra texture.
     */
    protected final Identifier TEXTURE_ELYTRA = new Identifier("textures/entity/elytra.png");

    private static final PlayerEntityModel<DummyPlayer> FAT = new PlayerEntityModel<>(0, false);
    private static final PlayerEntityModel<DummyPlayer> THIN = new PlayerEntityModel<>(0, true);

    @SuppressWarnings("deprecated")
    public RenderDummyPlayer(EntityRenderDispatcher renderer, EntityRendererRegistry.Context context) {
        this(renderer);
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public RenderDummyPlayer(EntityRenderDispatcher renderer) {
        super(renderer, (M)FAT, 0);
        addFeature(getElytraLayer());
    }

    protected FeatureRenderer<T, M> getElytraLayer() {
        final ElytraEntityModel<T> modelElytra = new ElytraEntityModel<>();
        return new FeatureRenderer<T, M>(this) {
            @Override
            public void render(T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
                ItemStack itemstack = entity.getEquippedStack(EquipmentSlot.CHEST);

                if (itemstack.getItem() == Items.ELYTRA) {
                    color4f(1, 1, 1, 1);
                    enableBlend();
                    blendFunc(SourceFactor.ONE, DestFactor.ZERO);

                    bindTexture(entity.getTextures().get(Type.ELYTRA).getId());

                    pushMatrix();
                    translatef(0, 0, 0.125F);

                    modelElytra.setAngles(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                    modelElytra.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

                    disableBlend();
                    popMatrix();
                }
            }

            @Override
            public boolean hasHurtOverlay() {
                return false;
            }
        };
    }

    @Override
    protected Identifier getTexture(T entity) {
        return entity.getTextures().get(Type.SKIN).getId();
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

        double offset = entity.getHeightOffset() + entity.y;

        if (entity.hasVehicle()) {
            offset = entity.getMountedHeightOffset() - entity.getHeight();
        }

        if (entity.isSleeping()) {
            y--;
            z += 0.75F;
        } else if (model.isSneaking) {
            y -= 0.125D;
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        pushMatrix();
        scalef(1, -1, 1);
        translated(0.001, offset, 0.001);

        if (entity.isSleeping()) {
            rotatef(-90, 1, 0, 0);
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

            BlockEntityRenderDispatcher dispatcher = BlockEntityRenderDispatcher.INSTANCE;

            MinecraftClient mc = MinecraftClient.getInstance();
            dispatcher.configure(entity.getEntityWorld(),
                    mc.getTextureManager(),
                    mc.getEntityRenderManager().getTextRenderer(),
                    mc.gameRenderer.getCamera(),
                    mc.hitResult);
            dispatcher.get(this).render(BedHead.instance, -0.5F, 0, 0, 0, -1);

            popMatrix();
            GL11.glPopAttrib();
        }
    }

    static class MrBoaty extends BoatEntity {
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
