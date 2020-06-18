package com.minelittlepony.hdskins.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmChatLinkScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;

public class ConfirmationScreen extends ConfirmScreen {

    private final Runnable action;
    private final Screen parent;

    public ConfirmationScreen(Screen parent, String title, Runnable action) {
        super(new LiteralText(title));
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
        addButton(new Label(this.width/2, height/2 - 10).setCentered())
            .getStyle().setText(getTitle().getString());

        addButton(new ButtonWidget(width/2 - 110, height/2 + 20, I18n.translate("gui.yes"), p -> {
            onClose();
            action.run();
        }));

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
