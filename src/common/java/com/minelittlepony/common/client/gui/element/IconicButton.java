package com.minelittlepony.common.client.gui.element;

import net.minecraft.client.Minecraft;

public class IconicButton extends Button {

    public IconicButton(int x, int y) {
        super(x, y, 20, 20);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);

        if (getStyle().hasIcon()) {
            Minecraft.getInstance().getItemRenderer().renderItemIntoGUI(getStyle().getIcon(), x + 2, y + 2);
        }
    }
}
