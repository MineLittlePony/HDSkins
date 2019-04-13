package com.minelittlepony.common.client.gui;

import javax.annotation.Nonnull;

public interface IField<T, V extends IField<T, V>> {

    V onChange(@Nonnull IChangeCallback<T> action);

    V setValue(T value);

    T getValue();

    @FunctionalInterface
    public interface IChangeCallback<T> {

        static <T> T none(T t) {
            return t;
        }

        /**
         * Performs this action now.
         *
         * @param  value New Value of the field being changed
         * @return       Adjusted value the field must take on
         */
        T perform(T value);
    }
}
