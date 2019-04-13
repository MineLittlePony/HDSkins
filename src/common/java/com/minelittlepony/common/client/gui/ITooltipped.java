package com.minelittlepony.common.client.gui;

import net.minecraft.client.Minecraft;

/**
 * Interface element that renders a tooltip when hovered.
 *
 * @author     Sollace
 *
 * @param  <T> The subclass element.
 */
public interface ITooltipped<T extends ITooltipped<T>> {
    /**
     * Draws this element's tooltip.
     */
    void renderToolTip(Minecraft mc, int mouseX, int mouseY);
}
