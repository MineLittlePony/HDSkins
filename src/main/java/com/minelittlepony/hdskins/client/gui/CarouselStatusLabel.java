package com.minelittlepony.hdskins.client.gui;

import java.util.List;

import com.minelittlepony.common.client.gui.ITextContext;
import com.minelittlepony.common.client.gui.dimension.Bounds;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public interface CarouselStatusLabel extends ITextContext {
    int LABEL_BACKGROUND = 0xB0000000;
    int LABEL_BORDER = 0xB0221111;
    int WHITE = 0xffffff;
    int RED = 0xff5555;

    boolean hasStatus();

    List<Text> getStatusLines();

    default int getLabelColor(Text status) {
        return WHITE;
    }

    default void renderStatus(MatrixStack matrices, Bounds bounds) {
        if (!hasStatus()) {
            return;
        }

        matrices.push();
        bounds.translate(matrices);
        matrices.translate(0, 0, 300);

        final int lineHeight = getFont().fontHeight;
        final int margin = 10;
        final List<Text> lines = getStatusLines();
        final int blockHeight = lines.size() * lineHeight;
        final int x = bounds.width / 2;
        int y = (bounds.height - blockHeight) / 2;

        final int border = 2;
        final int left = margin;
        final int top = y - margin;
        final int right = bounds.width - margin;
        final int bottom = top + blockHeight + margin + margin;

        DrawableHelper.fill(matrices, left + border,  top + border,    right - border, bottom - border, LABEL_BACKGROUND);
        DrawableHelper.fill(matrices, left + border,  top,             right - border, top + border, LABEL_BORDER);
        DrawableHelper.fill(matrices, left + border,  bottom - border, right - border, bottom, LABEL_BORDER);
        DrawableHelper.fill(matrices, left,           top + border,    left + border,  bottom - border, LABEL_BORDER);
        DrawableHelper.fill(matrices, right - border, top + border,    right,          bottom - border, LABEL_BORDER);

        for (Text line : lines) {
            drawCenteredLabel(matrices, line, x, y, getLabelColor(line), 0);
            y += lineHeight;
        }

        matrices.pop();
    }
}
