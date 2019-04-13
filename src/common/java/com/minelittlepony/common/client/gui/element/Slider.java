package com.minelittlepony.common.client.gui.element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.minelittlepony.common.client.gui.IField;

import java.util.function.Function;

/**
 * A slider for sliding.
 *
 * @author Sollace
 */
public class Slider extends Button implements IField<Float, Slider> {

    private float min;
    private float max;

    private float value;

    @Nonnull
    private IChangeCallback<Float> action = IChangeCallback::none;

    @Nullable
    private Function<Float, String> formatter;

    public Slider(int x, int y, float min, float max, float value) {
        super(x, y);

        this.min = min;
        this.max = max;
        this.value = value;
    }

    @Override
    public Slider onChange(@Nonnull IChangeCallback<Float> action) {
        this.action = action;
        return this;
    }

    public Slider setFormatter(@Nonnull Function<Float, String> formatter) {
        this.formatter = formatter;
        this.displayString = formatter.apply(getValue());

        return this;
    }

    @Override
    public Slider setValue(Float value) {
        value = clamp(value, min, max);
        value = (value - min) / (max - min);

        if (value != this.value) {
            this.value = action.perform(value);
        }

        if (formatter != null) {
            displayString = formatter.apply(getValue());
        }

        return this;
    }

    @Override
    public Float getValue() {
        return value;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        setValue((float)mouseX - (x + 4) / (float)(width - 8));
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double mouseDX, double mouseDY) {
        setValue((float)mouseX - (x + 4) / (float)(width - 8));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);

        int i = hovered ? 2 : 1;
        drawTexturedModalRect(x + (int)(value * (width - 8)), y, 0, 46 + i * 20, 4, 20);
        drawTexturedModalRect(x + (int)(value * (width - 8)) + 4, y, 196, 46 + i * 20, 4, 20);
    }

    @Override
    protected int getHoverState(boolean mouseOver) {
        return 0;
    }

    protected float clamp(float value, float min, float max) {
        return value < min ? min : value > max ? max : value;
    }
}
