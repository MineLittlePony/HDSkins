package com.minelittlepony.hdskins.client.gui;

import com.minelittlepony.common.client.gui.ITextContext;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class StatusBanner implements ITextContext {
    public static final Text HD_SKINS_UPLOAD = Text.translatable("hdskins.upload");
    public static final Text HD_SKINS_REQUEST = Text.translatable("hdskins.request");
    public static final Text HD_SKINS_FAILED = Text.translatable("hdskins.failed");

    private final SkinUploader uploader;

    private boolean showing;
    private float msgFadeOpacity = 0;
    private Text lastShownMessage = Text.empty();

    public StatusBanner(SkinUploader uploader) {
        this.uploader = uploader;
    }

    public void render(DrawContext context, float deltaTime, int width, int height) {

        boolean showBanner = uploader.hasBannerMessage();

        if (showBanner != showing) {
            showing = showBanner;
            if (showBanner) {
                lastShownMessage = uploader.getBannerMessage();
            }
        } else {
            if (showBanner) {
                Text updatedMessage = uploader.getBannerMessage();
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

        msgFadeOpacity = MathHelper.clamp(msgFadeOpacity, 0, 1);

        if (msgFadeOpacity > 0) {
            MatrixStack matrices = context.getMatrices();

            matrices.push();
            int opacity = (Math.min(180, (int)(msgFadeOpacity * 180)) & 255) << 24;

            context.fill(0, 0, width, height, opacity);

            if (showBanner || msgFadeOpacity >= 1) {
                int maxWidth = Math.min(width - 10, getFont().getWidth(lastShownMessage));
                int messageHeight = getFont().getWrappedLinesHeight(lastShownMessage.getString(), maxWidth) + getFont().fontHeight + 10;
                int blockY = (height - messageHeight) / 2;
                int blockX = (width - maxWidth) / 2;
                int padding = 6;

                drawTooltipDecorations(context, blockX - padding, blockY - padding, maxWidth + padding * 2, messageHeight + padding * 2);
                matrices.translate(0, 0, 400);

                if (lastShownMessage != HD_SKINS_UPLOAD && lastShownMessage != HD_SKINS_REQUEST) {
                    drawCenteredLabel(context, HD_SKINS_FAILED, width / 2, blockY, 0xffff55, 0);
                    drawTextBlock(context, lastShownMessage, blockX, blockY + getFont().fontHeight + 10, maxWidth, 0xff5555);
                } else {
                    uploader.tryClearStatus();
                    drawCenteredLabel(context, lastShownMessage, width / 2, height / 2, 0xffffff, 0);
                }
            }

            matrices.pop();
        }
    }

    public boolean isVisible() {
        return msgFadeOpacity > 0;
    }

    @SuppressWarnings("deprecation")
    static void drawTooltipDecorations(DrawContext context, int x, int y, int width, int height) {
        context.draw(() -> TooltipBackgroundRenderer.render(context, x, y, width, height, 400));
    }

}
