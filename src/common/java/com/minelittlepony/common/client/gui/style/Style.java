package com.minelittlepony.common.client.gui.style;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IItemProvider;

import java.util.List;

import com.google.common.base.Splitter;
import com.minelittlepony.common.client.gui.GameGui;

public class Style {

    private ItemStack icon = ItemStack.EMPTY;

    public int toolTipX = 0;
    public int toolTipY = 0;

    private List<String> tooltip;

    private String text = "";
    private int color;

    public ItemStack getIcon() {
        return icon;
    }

    public boolean hasIcon() {
        return !getIcon().isEmpty();
    }

    public Style setColor(int color) {
        this.color = color;

        return this;
    }

    public int getColor() {
        return color;
    }

    public Style setText(String text) {
        this.text = text;

        return this;
    }

    public String getText() {
        return GameGui.format(text);
    }

    public Style setIcon(IItemProvider iitem) {
        return setIcon(new ItemStack(iitem));
    }

    public Style setIcon(ItemStack stack) {
        icon = stack;

        return this;
    }

    public Style setIcon(ItemStack stack, int colour) {
        stack.getOrCreateChildTag("display").putInt("color", colour);
        return setIcon(stack);
    }

    /**
     * Sets the tooltip. The passed in value will be automatically translated and split into separate
     * lines.
     *
     * @param tooltip A tooltip translation string.
     */
    public Style setTooltip(String tooltip) {
        return setTooltip(Splitter.onPattern("\r?\n|\\\\n").splitToList(GameGui.format(tooltip)));
    }

    public Style setTooltip(String tooltip, int x, int y) {
        return setTooltip(tooltip).setTooltipOffset(x, y);
    }

    /**
     * Sets the tooltip text with a multi-line value.
     */
    public Style setTooltip(List<String> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public List<String> getTooltip() {
        return tooltip;
    }

    /**
     * Sets the tooltip offset from the original mouse position.
     */
    public Style setTooltipOffset(int x, int y) {
        toolTipX = x;
        toolTipY = y;
        return this;
    }
}
