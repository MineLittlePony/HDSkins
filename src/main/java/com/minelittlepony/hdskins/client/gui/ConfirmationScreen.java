package com.minelittlepony.hdskins.client.gui;

import javax.annotation.Nonnull;

import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.Label;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public class ConfirmationScreen extends GameGui {

    private final Runnable action;

    public ConfirmationScreen(@Nonnull Screen parent, String title, Runnable action) {
        super(new LiteralText(title), parent);

        this.action = action;
    }

    @Override
    public void init(MinecraftClient mc, int width, int height) {
        parent.init(mc, width, height);
        super.init(mc, width, height);
    }

    @Override
    protected void init() {
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
            .onClick(p -> client.openScreen(parent))
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
