package com.minelittlepony.hdskins.client.gui;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.Tooltip;
import com.minelittlepony.common.client.gui.element.Button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class ConfirmationScreen extends GameGui {

    private final Runnable action;

    private List<Component> message;

    public ConfirmationScreen(@NotNull Screen parent, Component title, Runnable action) {
        super(title, parent);

        this.action = action;
    }

    @Override
    public void init() {
        parent.init(width, height);

        message = Tooltip.of(getTitle(), width - 30).getLines();

        addButton(new Button(width/2 - 110, height/2 + 20, 100, 20))
            .onClick(_ -> {
                finish();
                action.run();
            })
            .getStyle()
                .setText("gui.yes");

        addButton(new Button(width/2 + 10, height/2 + 20, 100, 20))
            .onClick(_ -> finish())
            .getStyle()
                .setText("gui.no");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
        parent.extractRenderState(context, -1, -1, partialTicks);

        context.fill(0, 0, width, height, 0xC8000000);

        super.extractRenderState(context, mouseX, mouseY, partialTicks);

        int left = width / 2;
        int top = height / 2 - (message.size() * getFont().lineHeight);
        for (Component line : message) {
            drawCenteredLabel(context, line, left, top += getFont().lineHeight, CommonColors.WHITE);
        }
    }

    @Override
    public void tick() {
        parent.setDragging(false);
        parent.tick();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
