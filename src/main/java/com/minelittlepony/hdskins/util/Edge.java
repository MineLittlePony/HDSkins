package com.minelittlepony.hdskins.util;

import java.util.function.Consumer;

public abstract class Edge {

    private boolean previousState;

    private Consumer<Boolean> callback;

    public Edge(Consumer<Boolean> callback) {
        this.callback = callback;
    }

    public void update() {
        boolean state = nextState();

        if (state != previousState) {
            previousState = state;
            callback.accept(state);
        }
    }

    public boolean getState() {
        return previousState;
    }

    protected abstract boolean nextState();

}