package com.minelittlepony.hdskins.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Edge {

    private boolean previousState;

    private final Consumer<Boolean> callback;
    private final Supplier<Boolean> nextState;

    public Edge(Consumer<Boolean> callback, Supplier<Boolean> nextState) {
        this.callback = callback;
        this.nextState = nextState;
    }

    public void update() {
        boolean state = nextState.get();

        if (state != previousState) {
            previousState = state;
            callback.accept(state);
        }
    }

    public boolean getState() {
        return previousState;
    }
}