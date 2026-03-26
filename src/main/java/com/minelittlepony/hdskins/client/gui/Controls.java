package com.minelittlepony.hdskins.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.minelittlepony.common.client.gui.GameGui;
import com.mojang.blaze3d.platform.InputConstants;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class Controls {

    private final Minecraft client = Minecraft.getInstance();

    private final List<Edge> controls = new ArrayList<>();

    private final Edge jumpKey = addControl(this::jumpToggled, () -> client.options.keyJump.isDown());
    private final Edge sneakKey = addControl(this::sneakToggled, () -> client.options.keyShift.isDown());
    private final Edge ctrlKey = addControl(this::ctrlToggled, () -> GameGui.isKeyDown(InputConstants.KEY_LCONTROL) || GameGui.isKeyDown(InputConstants.KEY_RCONTROL));

    private boolean jumpState = false;
    private boolean sneakState = false;

    private final DualCarouselWidget<?> previewer;

    public Controls(DualCarouselWidget<?> previewer) {
        this.previewer = previewer;
        addControl(previewer::setSprinting, () -> client.options.keyUp.isDown() || client.options.keyDown.isDown());
    }

    protected Edge addControl(BooleanConsumer callback, BooleanSupplier nextState) {
        Edge control = new Edge(callback, nextState);
        controls.add(control);
        return control;
    }

    public void update() {
        KeyMapping.setAll();
        controls.forEach(Edge::update);
    }

    private void ctrlToggled(boolean ctrl) {
        if (ctrl) {
            if (sneakKey.getState()) {
                sneakState = !sneakState;
            }

            if (jumpKey.getState()) {
                jumpState = !jumpState;
            }
        }
    }

    private void jumpToggled(boolean jumping) {
        if (jumping && ctrlKey.getState()) {
            jumpState = !jumpState;
        }

        previewer.setJumping(jumping | jumpState);
    }

    private void sneakToggled(boolean sneaking) {
        if (sneaking && ctrlKey.getState()) {
            sneakState = !sneakState;
        }

        previewer.setSneaking(sneaking | sneakState);
    }

    static class Edge {
        private boolean previousState;

        private final BooleanConsumer callback;
        private final BooleanSupplier nextState;

        public Edge(BooleanConsumer callback, BooleanSupplier nextState) {
            this.callback = callback;
            this.nextState = nextState;
        }

        public void update() {
            if (nextState.getAsBoolean() != previousState) {
                previousState = !previousState;
                callback.accept(previousState);
            }
        }

        public boolean getState() {
            return previousState;
        }
    }
}
