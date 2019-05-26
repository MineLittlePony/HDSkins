package com.minelittlepony.hdskins.gui;

import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import java.util.Set;

import static com.mojang.blaze3d.platform.GlStateManager.*;

public class RenderPlayerModel<M extends EntityPlayerModel> extends LivingEntityRenderer<M, PlayerEntityModel<M>> {

    /**
     * The basic Elytra texture.
     */
    protected final Identifier TEXTURE_ELYTRA = new Identifier("textures/entity/elytra.png");

    private static final PlayerEntityModel<EntityPlayerModel> FAT = new PlayerEntityModel<>(0, false);
    private static final PlayerEntityModel<EntityPlayerModel> THIN = new PlayerEntityModel<>(0, true);

    @SuppressWarnings("unchecked")
    public RenderPlayerModel(EntityRenderDispatcher renderer) {
        super(renderer, (PlayerEntityModel<M>)FAT, 0);
        addFeature(getElytraLayer());
    }

    protected FeatureRenderer<M, PlayerEntityModel<M>> getElytraLayer() {
        final ElytraEntityModel<M> modelElytra = new ElytraEntityModel<>();
        return new FeatureRenderer<M, PlayerEntityModel<M>>(this) {
            @Override
            public void render(M entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
                ItemStack itemstack = entity.getEquippedStack(EquipmentSlot.CHEST);

                if (itemstack.getItem() == Items.ELYTRA) {
                    color4f(1, 1, 1, 1);
                    enableBlend();
                    blendFunc(SourceFactor.ONE, DestFactor.ZERO);

                    bindTexture(entity.getLocal(Type.ELYTRA).getTexture());

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
    protected Identifier getTexture(M entity) {
        return entity.getLocal(Type.SKIN).getTexture();
    }

    @Override
    protected boolean hasLabel(M entity) {
        return MinecraftClient.getInstance().player != null && super.hasLabel(entity);
    }

    // TODO: Where'd it go?
    //@Override
    //protected boolean setBrightness(M entity, float partialTicks, boolean combineTextures) {
    //    return MinecraftClient.getInstance().world != null && super.setBrightness(entity, partialTicks, combineTextures);
    //}

    @SuppressWarnings("unchecked")
    public <T extends PlayerEntityModel<M>> T getEntityModel(M entity) {
        return (T)(entity.usesThinSkin() ? THIN : FAT);
    }
    
    @Override
    protected boolean method_4055(M entity) {
        return MinecraftClient.getInstance().player != null && super.method_4055(entity);
    }

    @Override
    public void render(M entity, double x, double y, double z, float entityYaw, float partialTicks) {

        if (entity.isSleeping()) {
            BedHead.instance.render(entity);
        }

        if (entity.hasVehicle()) {
            MrBoaty.instance.render();
        }

        PlayerEntityModel<M> player = getEntityModel(entity);
        model = player;

        Set<PlayerModelPart> parts = MinecraftClient.getInstance().options.getEnabledPlayerModelParts();
        player.headwear.field_3664 = !parts.contains(PlayerModelPart.HAT);
        player.bodyOverlay.field_3664 = !parts.contains(PlayerModelPart.JACKET);
        player.leftLegOverlay.field_3664 = !parts.contains(PlayerModelPart.LEFT_PANTS_LEG);
        player.rightLegOverlay.field_3664 = !parts.contains(PlayerModelPart.RIGHT_PANTS_LEG);
        player.leftArmOverlay.field_3664 = !parts.contains(PlayerModelPart.LEFT_SLEEVE);
        player.rightArmOverlay.field_3664 = !parts.contains(PlayerModelPart.RIGHT_SLEEVE);
        player.isSneaking = entity.isSneaking();

        player.leftArmPose = ArmPose.EMPTY;
        player.rightArmPose = ArmPose.EMPTY;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        double offset = entity.getHeightOffset() + entity.y;

        if (entity.hasVehicle()) {
            offset = entity.getMountedHeightOffset() - entity.getHeight();
        }

        if (entity.isSleeping()) {
            y--;
            z += 0.75F;
        } else if (player.isSneaking) {
            y -= 0.125D;
        }

        pushMatrix();
        enableBlend();
        color4f(1, 1, 1, 0.3F);
        translated(0, offset, 0);

        if (entity.isSleeping()) {
            rotatef(-90, 1, 0, 0);
        }

        super.render(entity, x, y, z, entityYaw, partialTicks);

        color4f(1, 1, 1, 1);
        disableBlend();
        popMatrix();
        GL11.glPopAttrib();

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

            dispatcher.configure(entity.getEntityWorld(),
                    MinecraftClient.getInstance().getTextureManager(),
                    MinecraftClient.getInstance().getEntityRenderManager().getTextRenderer(),
                    MinecraftClient.getInstance().gameRenderer.getCamera(),
                    MinecraftClient.getInstance().hitResult);
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
