package com.minelittlepony.hdskins;

import net.minecraft.client.renderer.texture.NativeImage;

@FunctionalInterface
public interface ISkinModifier {

    void convertSkin(IDrawer skin);

    interface IDrawer {
        NativeImage getImage();

        void draw(int scale, int xFrom, int yFrom, int xToDelta, int yToDelta, int width, int height, boolean mirrorX, boolean mirrorY);
    }
}
