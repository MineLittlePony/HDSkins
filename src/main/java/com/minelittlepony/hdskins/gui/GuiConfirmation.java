package com.minelittlepony.hdskins.gui;

import javax.annotation.Nonnull;

import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.Label;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Screen;
import net.minecraft.network.chat.TranslatableComponent;

public class GuiConfirmation extends GameGui {

    private final Runnable action;

    @Nonnull
    private final Screen parent;

    public GuiConfirmation(@Nonnull Screen parent, String title, Runnable action) {
        super(new TranslatableComponent(title));

        this.parent = parent;
        this.action = action;
    }

    @Override
    public void init(MinecraftClient mc, int width, int height) {
        parent.init(mc, width, height);
        super.init(mc, width, height);
    }

    @Override
    protected void init() {
        addButton(new Label(width/2, height/2 - 10).setCentered())
            .getStyle().setText(getTitle().getString());

        addButton(new Button(width/2 - 110, height/2 + 20, 100, 20))
            .onClick(p -> {
                minecraft.openScreen(parent);
                action.run();
            })
            .getStyle()
                .setText("gui.yes");

        addButton(new Button(width/2 + 10, height/2 + 20, 100, 20))
            .onClick(p -> minecraft.openScreen(parent))
            .getStyle()
                .setText("gui.no");
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        parent.render(-1, -1, partialTicks);

        fill(0, 0, width, height, 0x88000000);

        super.render(mouseX, mouseY, partialTicks);
    }
}
