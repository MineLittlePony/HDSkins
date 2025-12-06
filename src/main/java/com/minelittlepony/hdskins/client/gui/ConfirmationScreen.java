package com.minelittlepony.hdskins.client.gui;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.Tooltip;
import com.minelittlepony.common.client.gui.element.Button;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class ConfirmationScreen extends GameGui {

    private final Runnable action;

    private List<Text> message;

    public ConfirmationScreen(@NotNull Screen parent, Text title, Runnable action) {
        super(title, parent);

        this.action = action;
    }

    @Override
    public void init() {
        parent.init(width, height);

        message = Tooltip.of(getTitle(), width - 30).getLines();

        addButton(new Button(width/2 - 110, height/2 + 20, 100, 20))
            .onClick(p -> {
                finish();
                action.run();
            })
            .getStyle()
                .setText("gui.yes");

        addButton(new Button(width/2 + 10, height/2 + 20, 100, 20))
            .onClick(p -> finish())
            .getStyle()
                .setText("gui.no");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        parent.render(context, -1, -1, partialTicks);

        context.fill(0, 0, width, height, 0xC8000000);

        super.render(context, mouseX, mouseY, partialTicks);

        int left = width / 2;
        int top = height / 2 - (message.size() * getFont().fontHeight);
        for (Text line : message) {
            drawCenteredLabel(context, line, left, top += getFont().fontHeight, Colors.WHITE);
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
