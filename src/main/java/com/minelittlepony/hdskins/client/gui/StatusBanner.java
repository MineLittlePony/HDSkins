package com.minelittlepony.hdskins.client.gui;

import org.joml.Matrix3x2fStack;

import com.minelittlepony.common.client.gui.ITextContext;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;

public class StatusBanner implements ITextContext {
    public static final Component HD_SKINS_UPLOAD = Component.translatable("hdskins.upload");
    public static final Component HD_SKINS_REQUEST = Component.translatable("hdskins.request");
    public static final Component HD_SKINS_FAILED = Component.translatable("hdskins.failed");

    private final SkinUploader uploader;

    private boolean showing;
    private float msgFadeOpacity = 0;
    private Component lastShownMessage = CommonComponents.EMPTY;

    public StatusBanner(SkinUploader uploader) {
        this.uploader = uploader;
    }

    public void extractRenderState(GuiGraphicsExtractor context, float deltaTime, int width, int height) {

        boolean showBanner = uploader.hasBannerMessage();

        if (showBanner != showing) {
            showing = showBanner;
            if (showBanner) {
                lastShownMessage = uploader.getBannerMessage();
            }
        } else {
            if (showBanner) {
                Component updatedMessage = uploader.getBannerMessage();
                if (updatedMessage != lastShownMessage) {
                    lastShownMessage = updatedMessage;
                }
            }
        }

        if (showing) {
            msgFadeOpacity += deltaTime / 6;
        } else {
            msgFadeOpacity -= deltaTime / 6;
        }

        msgFadeOpacity = Mth.clamp(msgFadeOpacity, 0, 1);

        if (msgFadeOpacity > 0) {
            Matrix3x2fStack matrices = context.pose();

            matrices.pushMatrix();
            int opacity = (Math.min(180, (int)(msgFadeOpacity * 180)) & 255) << 24;

            context.fill(0, 0, width, height, opacity);

            if (showBanner || msgFadeOpacity >= 1) {
                boolean showTitle = lastShownMessage != HD_SKINS_UPLOAD && lastShownMessage != HD_SKINS_REQUEST;
                int messageWidth = getFont().width(lastShownMessage);

                int maxWidth = Math.min(width - 10,
                        showTitle ? Math.max(getFont().width(HD_SKINS_FAILED), messageWidth) : messageWidth
                );
                int messageHeight = getFont().wordWrapHeight(lastShownMessage, maxWidth) + getFont().lineHeight + 10;
                int blockY = (height - messageHeight) / 2;
                int blockX = (width - maxWidth) / 2;
                int padding = 6;

                drawTooltipDecorations(context, blockX - padding, blockY - padding, maxWidth + padding * 2, messageHeight + padding * 2);

                if (showTitle) {
                    drawCenteredLabel(context, HD_SKINS_FAILED, width / 2, blockY, 0xFFFFFF55);
                    drawTextBlock(context, lastShownMessage, (width - messageWidth) / 2, blockY + getFont().lineHeight + 10, maxWidth, 0xFFFF5555);
                } else {
                    uploader.tryClearStatus();
                    drawCenteredLabel(context, lastShownMessage, width / 2, height / 2, CommonColors.WHITE);
                }
            }

            matrices.popMatrix();
        }
    }

    public boolean isVisible() {
        return msgFadeOpacity > 0;
    }

    static void drawTooltipDecorations(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        TooltipRenderUtil.extractTooltipBackground(context, x, y, width, height, null);
    }
}
