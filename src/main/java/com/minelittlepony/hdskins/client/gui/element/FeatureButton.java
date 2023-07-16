package com.minelittlepony.hdskins.client.gui.element;

import java.util.function.Consumer;

import com.minelittlepony.common.client.gui.element.Button;

public class FeatureButton extends Button implements ReactiveUiElement {
    private Consumer<FeatureButton> updateCallback;

    public FeatureButton(int x, int y, int width, int height) {
        super(x, y, width, height);
        setStyle(new FeatureStyle(this));
    }

    public void setLocked(boolean lock) {
        ((FeatureStyle)getStyle()).setLocked(lock);
    }

    public FeatureButton onUpdate(Consumer<FeatureButton> updateCallback) {
        this.updateCallback = updateCallback;
        return this;
    }

    @Override
    public void update() {
        if (updateCallback != null) {
            updateCallback.accept(this);
        }
    }
}