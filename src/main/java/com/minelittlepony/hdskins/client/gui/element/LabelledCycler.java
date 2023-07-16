package com.minelittlepony.hdskins.client.gui.element;

import java.util.function.Consumer;

import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.common.client.gui.element.Cycler;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class LabelledCycler extends Cycler implements ReactiveUiElement {

    private Consumer<LabelledCycler> updateCallback;

    public LabelledCycler(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        Bounds bounds = getBounds();
        int left = getStyle().getIcon().getBounds().right();
        drawScrollableText(context, textRenderer, getMessage(), bounds.left + left, bounds.top, bounds.right() - 2, bounds.bottom(), color);
    }

    public LabelledCycler onUpdate(Consumer<LabelledCycler> updateCallback) {
        this.updateCallback = updateCallback;
        return this;
    }

    @Override
    public void update() {
        if (updateCallback != null) {
            updateCallback.accept(this);
        }
    }
}
