package com.minelittlepony.hdskins.client.gui;

import com.minelittlepony.hdskins.HDSkinsServer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Public exposed stub for mods that want to create this screen dynamically.
 */
public class GuiSkinsStub extends Screen {
    private final Screen child;

    public GuiSkinsStub() {
        this(Minecraft.getInstance().screen);
    }

    public GuiSkinsStub(Screen parent) {
        super(Component.empty());
        this.child = GuiSkins.create(parent, HDSkinsServer.getInstance().getServers());
    }

    @Override
    public Component getTitle() {
        return child.getTitle();
    }

    @Override
    public void tick() {
        Minecraft.getInstance().setScreen(child);
    }

}
