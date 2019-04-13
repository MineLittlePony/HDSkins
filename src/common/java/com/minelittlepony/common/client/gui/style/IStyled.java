package com.minelittlepony.common.client.gui.style;

public interface IStyled<T extends IStyled<T>> extends IStyleFactory {

    default T setStyle(IStyleFactory factory) {
        return setStyle(factory.getStyle());
    }

    T setStyle(Style style);
}
