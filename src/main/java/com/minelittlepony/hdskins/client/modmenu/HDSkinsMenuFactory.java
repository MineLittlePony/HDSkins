package com.minelittlepony.hdskins.client.modmenu;

import com.minelittlepony.hdskins.client.gui.GuiSkins;
import com.minelittlepony.hdskins.client.HDSkins;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;

public class HDSkinsMenuFactory implements ModMenuApi {

    @Override
    public String getModId() {
        return "hdskins";
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return s -> GuiSkins.create(s, HDSkins.getInstance().getSkinServerList());
    }
}
