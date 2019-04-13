package com.minelittlepony.common.client.gui.element;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.minelittlepony.common.client.gui.ITooltipped;
import com.minelittlepony.common.client.gui.style.IStyled;
import com.minelittlepony.common.client.gui.style.Style;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class Button extends GuiButton implements ITooltipped<Button>, IStyled<Button> {

    private Style style = new Style();

    private static final Consumer<Button> NONE = v -> {};
    @Nonnull
    private Consumer<Button> action = NONE;

    public Button(int x, int y) {
        this(x, y, 200, 20);
    }

    public Button(int x, int y, int width, int height) {
        super(5000, x, y, width, height, "");
    }

    @SuppressWarnings("unchecked")
    public Button onClick(@Nonnull Consumer<? extends Button> callback) {
        action = (Consumer<Button>)callback;

        return this;
    }

    public Button setEnabled(boolean enable) {
        enabled = enable;
        return this;
    }

    @Override
    public Style getStyle() {
        return style;
    }

    @Override
    public Button setStyle(Style style) {
        this.style = style;

        return this;
    }

    @Override
    public void renderToolTip(Minecraft mc, int mouseX, int mouseY) {
        List<String> tooltip = getStyle().getTooltip();

        if (visible && isMouseOver() && tooltip != null) {
            mc.currentScreen.drawHoveringText(tooltip, mouseX + getStyle().toolTipX, mouseY + getStyle().toolTipY);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        displayString = getStyle().getText();
        packedFGColor = getStyle().getColor();

        super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        action.accept(this);
    }
}
