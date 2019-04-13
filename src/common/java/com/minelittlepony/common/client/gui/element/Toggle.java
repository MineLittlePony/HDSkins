package com.minelittlepony.common.client.gui.element;

import javax.annotation.Nonnull;

import com.minelittlepony.common.client.gui.IField;

public class Toggle extends Button implements IField<Boolean, Toggle> {

    private boolean on;

    @Nonnull
    private IChangeCallback<Boolean> action = IChangeCallback::none;

    public Toggle(int x, int y, boolean value) {
        super(x, y, 20, 20);

        on = value;
    }

    @Override
    public Toggle onChange(@Nonnull IChangeCallback<Boolean> action) {
        this.action = action;
        return this;
    }

    public Boolean getValue() {
        return on;
    }

    @Override
    public Toggle setValue(Boolean value) {
        if (value != on) {
            on = action.perform(value);
        }

        return this;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        setValue(!on);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);

        int i = hovered ? 2 : 1;
        float value = on ? 1 : 0;
        drawTexturedModalRect(x + (int)(value * (width - 8)), y, 0, 46 + i * 20, 4, 20);
        drawTexturedModalRect(x + (int)(value * (width - 8)) + 4, y, 196, 46 + i * 20, 4, 20);
    }

    @Override
    protected int getHoverState(boolean mouseOver) {
        return 0;
    }
}
