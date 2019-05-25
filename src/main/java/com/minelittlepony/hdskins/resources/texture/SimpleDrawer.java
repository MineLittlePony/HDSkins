package com.minelittlepony.hdskins.resources.texture;

import com.minelittlepony.hdskins.ISkinModifier;

public interface SimpleDrawer extends ISkinModifier.IDrawer {

    @Override
    default void draw(int scale, int xFrom, int yFrom, int xToDelta, int yToDelta, int widthIn, int heightIn, boolean mirrorX, boolean mirrorY) {
        getImage().method_4304(
                xFrom * scale, yFrom * scale, xToDelta * scale, yToDelta * scale,
                widthIn * scale, heightIn * scale, mirrorX, mirrorY);
    }
}
