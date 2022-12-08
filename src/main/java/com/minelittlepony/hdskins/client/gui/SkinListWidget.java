package com.minelittlepony.hdskins.client.gui;

import java.util.List;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.common.client.gui.element.Button;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SkinListWidget extends DrawableHelper {

    private final PlayerPreview previewer;

    private final MinecraftClient client = MinecraftClient.getInstance();

    private final Bounds containerBounds;
    private final Bounds bounds = new Bounds(0, 0, 0, 32);

    private float prevScrollPosition;
    private float scrollPosition;
    private int targetScrollPosition;

    private Button scrollLeft;
    private Button scrollRight;

    public SkinListWidget(PlayerPreview previewer, Bounds bounds) {
        this.previewer = previewer;
        this.containerBounds = bounds;
    }

    public void init(GuiSkins screen) {
        bounds.width = containerBounds.width - 20;
        bounds.left = containerBounds.left + 10;
        bounds.top = containerBounds.top + containerBounds.height - bounds.height;

        screen.addButton(scrollLeft = new Button(bounds.left - 10, bounds.top, 10, bounds.height))
            .onClick(sender -> scrollBy(-1))
            .getStyle().setText("<");
        screen.addButton(scrollRight = new Button(bounds.left + bounds.width, bounds.top, 10, bounds.height))
            .onClick(sender -> scrollBy(1))
            .getStyle().setText(">");
    }

    private void scrollBy(int steps) {
        targetScrollPosition += steps;
        int skins = previewer.getServerTextures().getProfileSkins(previewer.getActiveSkinType()).size();

        int pageSize = bounds.width / bounds.height;

        targetScrollPosition = skins < pageSize ? 0 : MathHelper.clamp(targetScrollPosition, 0, skins);
    }

    private float getScrollOffset() {
        return -MathHelper.lerp(MinecraftClient.getInstance().getTickDelta(), prevScrollPosition, scrollPosition) * bounds.height;
    }

    public void render(DummyPlayer player, MatrixStack matrices, int mouseX, int mouseY) {

        prevScrollPosition = scrollPosition;
        if (targetScrollPosition != scrollPosition) {
            if (scrollPosition > targetScrollPosition) {
                if (scrollPosition - targetScrollPosition < 0.2F) {
                    scrollPosition = targetScrollPosition;
                } else {
                    scrollPosition -= 0.1F;
                }
            }
            if (scrollPosition < targetScrollPosition) {
                if (targetScrollPosition - scrollPosition < 0.2F) {
                    scrollPosition = targetScrollPosition;
                } else {
                    scrollPosition += 0.1F;
                }
            }
        }

        List<Skin> skins = previewer.getServerTextures().getProfileSkins(previewer.getActiveSkinType());

        scrollLeft.setVisible(!skins.isEmpty());
        scrollLeft.setEnabled(!skins.isEmpty() && scrollPosition > 0);
        scrollRight.setVisible(!skins.isEmpty());

        if (skins.isEmpty()) {
            return;
        }

        int pageSize = bounds.width / bounds.height;

        scrollRight.setEnabled(!skins.isEmpty() && skins.size() >= pageSize && skins.size() > scrollPosition);

        int frameWidth = bounds.height;

        boolean sneaking = player.isSneaking();
        if (sneaking) {
            player.setSneaking(false);
        }

        matrices.push();

        bounds.translate(matrices);
        matrices.translate(getScrollOffset(), 0, 0);

        fill(matrices, 0, frameWidth, bounds.width, 0, 0xA0000000);

        int index = (int)(mouseX - (bounds.left + getScrollOffset())) / frameWidth;

        boolean hovered = bounds.contains(mouseX, mouseY);

        if (hovered && index < skins.size()) {
            fill(matrices, index * frameWidth, 0, (index + 1) * frameWidth, frameWidth, 0xA0AAAAAA);
        }

        try {
            for (int i = 0; i < skins.size(); i++) {
                Skin skin = skins.get(i);

                fill(matrices, (i * frameWidth), 0, ((i + 1) * frameWidth), frameWidth, 0xA0000000);

                if (skin.isReady()) {
                    player.setOverrideTextures(new PreviousServerPlayerSkins(skin));

                    float limbD = player.limbDistance;
                    int y = frameWidth;
                    if (hovered && i == index) {
                        y -= 3;
                        player.limbDistance = 1F;
                    }

                    renderPlayerModel(matrices, player, (i * frameWidth) + frameWidth / 2, y, 13);
                    player.limbDistance = limbD;
                }

                if (skin.active()) {
                    fill(matrices, (i * frameWidth), 1, (i * frameWidth) + 1, frameWidth, 0xFFFFFFFF);
                    fill(matrices, ((i + 1) * frameWidth), 1, ((i + 1) * frameWidth) - 1, frameWidth, 0xFFFFFFFF);
                    fill(matrices, (i * frameWidth), frameWidth - 1, ((i + 1) * frameWidth), frameWidth, 0xFFFFFFFF);
                    fill(matrices, (i * frameWidth), 0, ((i + 1) * frameWidth), 1, 0xFFFFFFFF);
                }
            }
        } finally {
            player.setOverrideTextures(PlayerSkins.EMPTY);

            if (sneaking) {
                player.setSneaking(true);
            }
        }

        matrices.pop();
    }

    public boolean mouseClicked(SkinUploader uploader, double mouseX, double mouseY, int button) {

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        int frameWidth = bounds.height;

        if (!bounds.contains(mouseX, mouseY)) {
            return false;
        }

        mouseX -= bounds.left + getScrollOffset();

        int index = (int)(mouseX / frameWidth);

        if (index >= previewer.getServerTextures().getProfileSkins(previewer.getActiveSkinType()).size()) {
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
        float swingProgress = thePlayer.handSwingProgress;
        thePlayer.handSwingProgress = 0;

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
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(220));

        DiffuseLighting.method_34742();

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        renderPlayerEntity(matrixStack, thePlayer, immediate, dispatcher);

        immediate.draw();

        matrixStack.pop();
        modelStack.pop();
        RenderSystem.applyModelViewMatrix();
        DiffuseLighting.enableGuiDepthLighting();

        thePlayer.handSwingProgress = swingProgress;
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
