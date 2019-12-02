package com.minelittlepony.hdskins.dummy;

import static com.mojang.blaze3d.systems.RenderSystem.*;

import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class DummyPlayerElytraLayer<T extends DummyPlayer, M extends PlayerEntityModel<T>> extends FeatureRenderer<T, M> {

    private final ElytraEntityModel<T> modelElytra = new ElytraEntityModel<>();

    public DummyPlayerElytraLayer(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack stack, VertexConsumerProvider renderContext, int lightValue, T entity, float limbDistance, float limbAngle, float tickDelta, float age, float headYaw, float headPitch) {
        ItemStack itemstack = entity.getEquippedStack(EquipmentSlot.CHEST);

        if (itemstack.getItem() == Items.ELYTRA) {
            color4f(1, 1, 1, 1);
            enableBlend();
            blendFunc(SourceFactor.ONE, DestFactor.ZERO);

            Identifier texture = entity.getTextures().get(SkinType.ELYTRA).getId();

            stack.push();
            stack.translate(0, 0, 0.125D);

            this.getModel().copyStateTo(modelElytra);
            modelElytra.method_17079(entity, limbDistance, limbAngle, age, headYaw, headPitch);

            VertexConsumer vertexConsumer = ItemRenderer.getArmorVertexConsumer(renderContext, modelElytra.getLayer(texture), false, itemstack.hasEnchantmentGlint());
            modelElytra.render(stack, vertexConsumer, lightValue, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);

            disableBlend();
            stack.pop();
        }
    }
}
