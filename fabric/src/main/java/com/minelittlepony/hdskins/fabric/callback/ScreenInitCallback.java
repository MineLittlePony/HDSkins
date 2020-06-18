package com.minelittlepony.hdskins.fabric.callback;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;

import java.util.function.Consumer;

public interface ScreenInitCallback {

    Event<ScreenInitCallback> EVENT = EventFactory.createArrayBacked(ScreenInitCallback.class, listeners -> (screen, addButton) -> {
        for (ScreenInitCallback callback : listeners) {
            callback.onScreenInit(screen, addButton);
        }
    });

    void onScreenInit(Screen screen, Consumer<AbstractButtonWidget> addButton);
}