package io.github.prospector.modmenu.api;

import java.util.function.Function;
import net.minecraft.client.gui.screen.Screen;

public interface ModMenuApi {
    String getModId();

    default Function<Screen, ? extends Screen> getConfigScreenFactory() {
        return screen -> null;
    }
}
