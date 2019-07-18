package com.minelittlepony.hdskins.dummy;

import static com.mojang.blaze3d.platform.GlStateManager.blendFunc;
import static com.mojang.blaze3d.platform.GlStateManager.color4f;
import static com.mojang.blaze3d.platform.GlStateManager.disableBlend;
import static com.mojang.blaze3d.platform.GlStateManager.enableBlend;
import static com.mojang.blaze3d.platform.GlStateManager.popMatrix;
import static com.mojang.blaze3d.platform.GlStateManager.pushMatrix;
import static com.mojang.blaze3d.platform.GlStateManager.translatef;

import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;

import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class DummyPlayerElytraLayer<T extends DummyPlayer, M extends PlayerEntityModel<T>> extends FeatureRenderer<T, M> {

    private final ElytraEntityModel<T> modelElytra = new ElytraEntityModel<>();

    public DummyPlayerElytraLayer(FeatureRendererContext<T, M> context) {
        super(context);
    }

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
}
