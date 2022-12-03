package com.minelittlepony.hdskins.client.gui;

import java.util.List;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.SkinUploader;
import com.minelittlepony.hdskins.client.dummy.*;
import com.minelittlepony.hdskins.client.resources.PreviousServerPlayerSkins;
import com.minelittlepony.hdskins.client.resources.ServerPlayerSkins.Skin;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.RotationAxis;

public class SkinListWidget extends DrawableHelper {

    private final PlayerPreview previewer;

    private final MinecraftClient client = MinecraftClient.getInstance();

    public SkinListWidget(PlayerPreview previewer) {
        this.previewer = previewer;
    }

    public void render(DummyPlayer player, MatrixStack matrices, int mouseX, int mouseY) {

        List<Skin> skins = previewer.getServerTextures().getProfileSkins(previewer.getActiveSkinType());

        int frameWidth = 16;

        boolean sneaking = player.isSneaking();
        if (sneaking) {
            player.setSneaking(false);
        }

        fill(matrices, 0, -frameWidth - 3, 10000, 0, 0xA0000000);

        mouseY += frameWidth;

        matrices.translate(2, -2, 0);

        if (mouseY >= 0 && mouseY <= frameWidth) {
            int index = mouseX / frameWidth;

            if (index >= 0 && index <= skins.size()) {
                fill(matrices, index * frameWidth, -frameWidth, (index + 1) * frameWidth, 0, 0xA0AAAAAA);
            }
        }

        try {
            matrices.translate(0, -1, 0);

            for (int i = 0; i < skins.size(); i++) {
                Skin skin = skins.get(i);

                fill(matrices, (i * frameWidth), -frameWidth + 1, ((i + 1) * frameWidth), 0, 0xA0000000);

                if (skin.isReady()) {
                    player.setOverrideTextures(new PreviousServerPlayerSkins(skin));
                    renderPlayerModel(matrices, player, (i * frameWidth) + frameWidth / 2, 0, 7);
                }
            }
        } finally {
            player.setOverrideTextures(PlayerSkins.EMPTY);

            if (sneaking) {
                player.setSneaking(true);
            }
        }
    }

    public boolean mouseClicked(SkinUploader uploader, double mouseX, double mouseY, int button) {

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        int frameHeight = 64;
        int frameWidth = 64;

        mouseY += frameHeight;

        if (mouseY < 0 || mouseY > frameHeight) {
            return false;
        }

        int index = (int)(mouseX / frameWidth);

        if (index < 0 || index > previewer.getServerTextures().getProfileSkins(previewer.getActiveSkinType()).size()) {
            return false;
        }

        GameGui.playSound(SoundEvents.UI_BUTTON_CLICK);

        return uploader.getGateway().filter(gateway -> {
            return gateway.getProfile(previewer.getProfile()).getNow(Optional.empty()).filter(profile -> {
                SkinType type = previewer.getActiveSkinType();
                if (index >= 0 && index <= profile.getSkins(type).size()) {
                    gateway.swapSkin(profile, type, index, uploader::setBannerMessage).thenRunAsync(uploader::scheduleReload, client);
                    return true;
                }

                return false;
            }).isPresent();
        }).isPresent();
    }

    private void renderPlayerModel(MatrixStack matrixStack, DummyPlayer thePlayer, float xPosition, float yPosition, float scale) {
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        if (dispatcher.getRenderer(thePlayer) == null) {
            HDSkins.LOGGER.warn("Entity " + thePlayer.toString() + " does not have a valid renderer. Did resource loading fail?");
            return;
        }

        thePlayer.setHeadYaw(0);
        thePlayer.setPitch(0);

        MatrixStack modelStack = RenderSystem.getModelViewStack();
        modelStack.push();
        modelStack.translate(xPosition, yPosition, 1050);
        modelStack.scale(1, 1, -1);
        RenderSystem.applyModelViewMatrix();

        matrixStack.push();
        matrixStack.translate(0, 0, 1000);
        matrixStack.scale(scale, scale, scale);

        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));

        DiffuseLighting.method_34742();

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        renderPlayerEntity(matrixStack, thePlayer, immediate, dispatcher);

        immediate.draw();

        matrixStack.pop();
        modelStack.pop();
        RenderSystem.applyModelViewMatrix();
        DiffuseLighting.enableGuiDepthLighting();
    }

    protected void renderPlayerEntity(MatrixStack matrixStack, DummyPlayer thePlayer, VertexConsumerProvider renderContext, EntityRenderDispatcher dispatcher) {
        float x = 0;
        float y = 0;
        float z = 0;

        if (thePlayer.isSneaking()) {
            y -= 0.125D;
        }

        matrixStack.push();
        matrixStack.translate(0.001, 0, 0.001);

        DummyWorld.fillWith(Blocks.AIR.getDefaultState());

        if (thePlayer.getVelocity().x >= 100) {
            thePlayer.addVelocity(-100, 0, 0);
        }

        Entity camera = client.getCameraEntity();
        client.setCameraEntity(thePlayer);

        dispatcher.render(thePlayer, x, y, z, 0, 1, matrixStack, renderContext, 0xF000F0);

        client.setCameraEntity(camera);

        matrixStack.pop();
    }

}
