package com.minelittlepony.hdskins.resources.texture;

import com.minelittlepony.common.util.TextureConverter;

public interface HDDrawer extends TextureConverter.Drawer {

    @Override
    default void copy(int xFrom, int yFrom, int xOffset, int yOffset, int width, int height, boolean mirrorX, boolean mirrorY) {
        int scale = getImage().getWidth() / 64;
        getImage().method_4304(
                xFrom * scale, yFrom * scale, xOffset * scale, yOffset * scale,
                width * scale, height * scale, mirrorX, mirrorY);
    }
}
