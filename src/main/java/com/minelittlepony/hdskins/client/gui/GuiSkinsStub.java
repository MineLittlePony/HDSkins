package com.minelittlepony.hdskins.client.gui;

import java.util.concurrent.CompletableFuture;

import com.minelittlepony.hdskins.HDSkinsServer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Public exposed stub for mods that want to create this screen dynamically.
 */
public class GuiSkinsStub extends Screen {
    private final Screen child;

    public GuiSkinsStub() {
        this(MinecraftClient.getInstance().currentScreen);
    }

    public GuiSkinsStub(Screen parent) {
        super(Text.empty());
        this.child = GuiSkins.create(parent, HDSkinsServer.getInstance().getServers());
    }

    @Override
    public Text getTitle() {
        return child.getTitle();
    }

    @Override
    public void onDisplayed() {
        CompletableFuture.runAsync(() -> {
            MinecraftClient.getInstance().setScreen(child);
        }, MinecraftClient.getInstance());
    }

}
