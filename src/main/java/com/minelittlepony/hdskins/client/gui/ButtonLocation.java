package com.minelittlepony.hdskins.client.gui;

public enum ButtonLocation {
    ICON(Axis.START, Axis.START),
    REPLACE_FRIENDS(Axis.START, Axis.START),
    REPLACE_LANGUAGE(Axis.START, Axis.START),
    REPLACE_ACCESSIBILITY(Axis.START, Axis.START),
    TOP_LEFT(Axis.START, Axis.START),
    TOP_RIGHT(Axis.END, Axis.START),
    BOTTOM_LEFT(Axis.START, Axis.END),
    BOTTOM_RIGHT(Axis.END, Axis.END);

    public final Axis x;
    public final Axis y;

    ButtonLocation(Axis x, Axis y) {
        this.x = x;
        this.y = y;
    }

    public interface Axis {
        Axis START = (a, _) -> a;
        Axis END = (_, b) -> b;
        int pick(int a, int b);
    }
}
