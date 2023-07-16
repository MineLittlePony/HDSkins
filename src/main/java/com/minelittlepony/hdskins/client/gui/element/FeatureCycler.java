package com.minelittlepony.hdskins.client.gui.element;

import java.util.function.Consumer;

import com.minelittlepony.common.client.gui.element.Cycler;
import com.minelittlepony.common.client.gui.style.Style;

public class FeatureCycler extends Cycler implements ReactiveUiElement {
    private Consumer<FeatureCycler> updateCallback;

    public FeatureCycler(int x, int y) {
        super(x, y, 20, 20);
        setStyle(new FeatureStyle(this));
        setStyles(getStyle());
    }

    public void setLocked(boolean lock) {
        for (Style i : getStyles()) {
            ((FeatureStyle)i).setLocked(lock);
        }
    }
    public FeatureCycler onUpdate(Consumer<FeatureCycler> updateCallback) {
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