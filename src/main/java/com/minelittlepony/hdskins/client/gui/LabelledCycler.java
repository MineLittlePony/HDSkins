package com.minelittlepony.hdskins.client.gui;

import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.common.client.gui.element.Cycler;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class LabelledCycler extends Cycler {

    public LabelledCycler(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        Bounds bounds = getBounds();
        int left = getStyle().getIcon().getBounds().right();
        drawScrollableText(context, textRenderer, getMessage(), bounds.left + left, bounds.top, bounds.right() - 2, bounds.bottom(), color);
    }
}
