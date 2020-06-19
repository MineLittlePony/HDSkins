package com.minelittlepony.hdskins.fabric.modmenu;

import io.github.prospector.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.Function;

public class HDSkinsMenuFactory implements ModMenuApi {

    @Override
    public String getModId() {
        return "hdskins";
    }

    @Override
    public Function<Screen, ? extends Screen> getConfigScreenFactory() {
        return s -> s;//GuiSkins.create(s, HDSkins.getInstance().getSkinCache().getServerList());
    }
}
