package com.minelittlepony.hdskins.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;

public class Controls {

    private final MinecraftClient client = MinecraftClient.getInstance();

    private final List<Edge> controls = new ArrayList<>();

    private final Edge jumpKey = addControl(this::jumpToggled, () -> client.options.jumpKey.isPressed());
    private final Edge sneakKey = addControl(this::sneakToggled, () -> client.options.sneakKey.isPressed());
    private final Edge ctrlKey = addControl(this::ctrlToggled, Screen::hasControlDown);

    private boolean jumpState = false;
    private boolean sneakState = false;

    private final DualPreview previewer;

    public Controls(DualPreview previewer) {
        this.previewer = previewer;
        addControl(previewer::setSprinting, () -> client.options.forwardKey.isPressed() || client.options.backKey.isPressed());
    }

    protected Edge addControl(BooleanConsumer callback, BooleanSupplier nextState) {
        Edge control = new Edge(callback, nextState);
        controls.add(control);
        return control;
    }

    public void update() {
        KeyBinding.updatePressedStates();
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
