package com.minelittlepony.hdskins.util.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public class ClippingSpace {

    public static void renderClipped(int x, int y, int width, int height, Runnable renderTask) {
        enableClipRegion(x, y, width, height);

        renderTask.run();

        disableClipRegion();
    }

    private static void enableClipRegion(int x, int y, int width, int height) {
        Window window = MinecraftClient.getInstance().window;
        double f = window.getScaleFactor();
        int windowHeight = (int)Math.round(window.getScaledHeight() * f);

        x *= f;
        y *= f;
        width *= f;
        height *= f;

        GL11.glScissor(
                (int)Math.round(x),
                windowHeight - height - y,
                (int)Math.round(width),
                (int)Math.round(height)
        );
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    private static void disableClipRegion() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
