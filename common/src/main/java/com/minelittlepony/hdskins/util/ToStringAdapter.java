package com.minelittlepony.hdskins.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

public class ToStringAdapter<T> extends TypeAdapter<T> {

    private final Function<T, String> toString;
    private final Function<String, T> fromString;

    public ToStringAdapter(Function<T, String> toString, Function<String, T> fromString) {
        this.toString = toString;
        this.fromString = fromString;
    }

    public ToStringAdapter(Function<String, T> function) {
        this(Objects::toString, function);
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        out.value(value == null ? null : toString.apply(value));
    }

    @Override
    public T read(JsonReader in) throws IOException {
        return in.peek() == JsonToken.NULL ? null : fromString.apply(in.nextString());
    }
}
