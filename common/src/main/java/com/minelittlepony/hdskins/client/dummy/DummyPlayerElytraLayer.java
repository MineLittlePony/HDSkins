package com.minelittlepony.hdskins.client.dummy;

import com.minelittlepony.hdskins.skins.SkinType;
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
            Identifier texture = entity.getTextures().get(SkinType.ELYTRA).getId();

            stack.push();
            stack.translate(0, 0, 0.125D);

            getContextModel().copyStateTo(modelElytra);
            modelElytra.setAngles(entity, limbDistance, limbAngle, age, headYaw, headPitch);

            VertexConsumer vertexConsumer = ItemRenderer.getArmorVertexConsumer(renderContext, modelElytra.getLayer(texture), false, itemstack.hasEnchantmentGlint());
            modelElytra.render(stack, vertexConsumer, lightValue, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
            stack.pop();
        }
    }
}
