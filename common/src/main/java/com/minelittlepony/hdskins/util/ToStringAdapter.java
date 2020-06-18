package com.minelittlepony.hdskins.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.function.Function;

public class ToStringAdapter<T> extends TypeAdapter<T> {

    private final Function<String, T> fromString;
    private final Function<T, String> toString;

    public ToStringAdapter(Function<T, String> toString, Function<String, T> fromString) {
        this.fromString = fromString;
        this.toString = toString;
    }

    public ToStringAdapter(Function<String, T> function) {
        this(Object::toString, function);
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
