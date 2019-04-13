package com.minelittlepony.common.client.gui.style;

import com.minelittlepony.common.util.MoreStreams;

public interface IMultiStyled<T extends IMultiStyled<T>> {

    default T setStyles(IStyleFactory... styles) {
        return setStyles(MoreStreams.map(styles, IStyleFactory::getStyle, Style[]::new));
    }

    T setStyles(Style... styles);

    Style[] getStyles();
}
