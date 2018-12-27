package com.minelittlepony.gui;


import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiCheckBox;

/**
 * Checkbox that supports a gui action when it changes.
 *
 * @author Sollace
 *
 */
public class Checkbox extends GuiCheckBox implements IActionable, IGuiTooltipped<Checkbox> {

    private int tipX = 0;
    private int tipY = 0;

    private List<String> tooltip = null;

    private final IGuiCallback<Boolean> action;

    public Checkbox(int x, int y, String displayString, boolean value, IGuiCallback<Boolean> callback) {
        super(0, x, y, I18n.format(displayString), value);
        action = callback;
    }

    @Override
    public void perform() {
        setIsChecked(action.perform(!isChecked()));
    }

    @Override
    public Checkbox setTooltip(List<String> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    @Override
    public void renderToolTip(Minecraft mc, int mouseX, int mouseY) {
        if (visible && isMouseOver() && tooltip != null) {
            mc.currentScreen.drawHoveringText(tooltip, mouseX + tipX, mouseY + tipY);
        }
    }

    @Override
    public Checkbox setTooltipOffset(int x, int y) {
        tipX = x;
        tipY = y;
        return this;
    }
}
