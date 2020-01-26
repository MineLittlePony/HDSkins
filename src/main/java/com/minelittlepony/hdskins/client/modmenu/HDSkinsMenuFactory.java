package com.minelittlepony.hdskins.client.modmenu;

import net.minecraft.client.gui.screen.Screen;

import java.util.function.Function;

import com.minelittlepony.hdskins.client.HDSkins;

import io.github.prospector.modmenu.api.ModMenuApi;

public class HDSkinsMenuFactory implements ModMenuApi {

    @Override
    public String getModId() {
        return "hdskins";
    }

    @Override
    public Function<Screen, ? extends Screen> getConfigScreenFactory() {
        return HDSkins.getInstance().getSkinServerList()::createSkinsGui;
    }
}
