package com.minelittlepony.hdskins.client.modmenu;

import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.client.gui.GuiSkins;
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
        return s -> GuiSkins.create(s, HDSkins.getInstance().getSkinServerList());
    }
}
