package com.minelittlepony.hdskins.client.gui;

import org.jetbrains.annotations.NotNull;

import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.Label;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class ConfirmationScreen extends GameGui {

    private final Runnable action;

    public ConfirmationScreen(@NotNull Screen parent, Text title, Runnable action) {
        super(title, parent);

        this.action = action;
    }

    @Override
    public void init() {
        parent.init(client, width, height);

        addButton(new Label(this.width/2, height/2 - 10).setCentered())
            .getStyle().setText(getTitle());

        addButton(new Button(width/2 - 110, height/2 + 20, 100, 20))
            .onClick(p -> {
                finish();
                action.run();
            })
            .getStyle()
                .setText("gui.yes");

        addButton(new Button(width/2 + 10, height/2 + 20, 100, 20))
            .onClick(p -> client.setScreen(parent))
            .getStyle()
                .setText("gui.no");
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        parent.render(matrices, -1, -1, partialTicks);

        fill(matrices, 0, 0, width, height, 0x88000000);

        super.render(matrices, mouseX, mouseY, partialTicks);
    }
}
