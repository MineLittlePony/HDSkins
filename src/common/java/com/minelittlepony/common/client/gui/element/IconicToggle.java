package com.minelittlepony.common.client.gui.element;

import javax.annotation.Nonnull;

import com.minelittlepony.common.client.gui.IField;
import com.minelittlepony.common.client.gui.style.IMultiStyled;
import com.minelittlepony.common.client.gui.style.Style;

public class IconicToggle extends IconicButton implements IMultiStyled<IconicToggle>, IField<Integer, IconicToggle> {

    private Style[] styles = new Style[] {
            getStyle()
    };

    private int value;

    @Nonnull
    private IChangeCallback<Integer> action = IChangeCallback::none;

    public IconicToggle(int x, int y) {
        super(x, y);
    }

    @Override
    public IconicToggle onChange(IChangeCallback<Integer> action) {
        this.action = action;

        return this;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    public IconicToggle setValue(Integer value) {
        if (this.value != value) {
            this.value = action.perform(value) % styles.length;
            this.setStyle(styles[this.value]);
        }

        return this;
    }

    @Override
    public IconicToggle setStyles(Style... styles) {
        this.styles = styles;

        value = value % styles.length;
        setStyle(styles[value]);

        return this;
    }

    @Override
    public Style[] getStyles() {
        return styles;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setValue(value + 1);
        super.onClick(mouseX, mouseY);
    }

}
