package com.minelittlepony.hdskins.client.gui;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.Tooltip;
import com.minelittlepony.common.client.gui.element.Button;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class ConfirmationScreen extends GameGui {

    private final Runnable action;

    private List<Text> message;

    public ConfirmationScreen(@NotNull Screen parent, Text title, Runnable action) {
        super(title, parent);

        this.action = action;
    }

    @Override
    public void init() {
        parent.init(client, width, height);

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
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {

        parent.render(matrices, -1, -1, partialTicks);
        matrices.push();
        matrices.translate(0, 0, 300);

        DrawableHelper.fill(matrices, 0, 0, width, height, 0xC8000000);

        int left = width / 2;
        int top = height / 2 - (message.size() * getFont().fontHeight);
        for (Text line : message) {
            drawCenteredLabel(matrices, line, left, top += getFont().fontHeight, 0xFFFFFFFF, 0);
        }

        super.render(matrices, mouseX, mouseY, partialTicks);
        matrices.pop();
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
