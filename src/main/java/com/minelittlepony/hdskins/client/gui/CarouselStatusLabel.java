package com.minelittlepony.hdskins.client.gui;

import java.util.List;

import org.joml.Matrix3x2fStack;

import com.minelittlepony.common.client.gui.ITextContext;
import com.minelittlepony.common.client.gui.dimension.Bounds;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public interface CarouselStatusLabel extends ITextContext {
    int LABEL_BACKGROUND = 0xB0000000;
    int LABEL_BORDER = 0xB0221111;
    int WHITE = Colors.WHITE;
    int RED = 0xffff5555;

    boolean hasStatus();

    List<Text> getStatusLines();

    default int getLabelColor(Text status) {
        return WHITE;
    }

    default void renderStatus(DrawContext context, Bounds bounds) {
        if (!hasStatus()) {
            return;
        }

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        bounds.translate(matrices);

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

        context.fill(left + border,  top + border,    right - border, bottom - border, LABEL_BACKGROUND);
        context.fill(left + border,  top,             right - border, top + border, LABEL_BORDER);
        context.fill(left + border,  bottom - border, right - border, bottom, LABEL_BORDER);
        context.fill(left,           top + border,    left + border,  bottom - border, LABEL_BORDER);
        context.fill(right - border, top + border,    right,          bottom - border, LABEL_BORDER);


        for (int i = 0; i < 9000; i++) {
            context.state.goUpLayer();
        }

        for (Text line : lines) {
            drawCenteredLabel(context, line, x, y, getLabelColor(line));
            y += lineHeight;
        }

        matrices.popMatrix();
    }
}
