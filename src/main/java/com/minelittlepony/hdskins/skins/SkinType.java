package com.minelittlepony.hdskins.skins;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@JsonAdapter(SkinType.Adapter.class)
public class SkinType implements Comparable<SkinType> {

    private static final Map<String, SkinType> VALUES = new HashMap<>();

    public static final SkinType SKIN = of("SKIN");
    public static final SkinType CAPE = of("CAPE");
    public static final SkinType ELYTRA = of("ELYTRA");

    private final String name;

    private SkinType(String name) {
        this.name = name;
    }

    public final String name() {
        return name;
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof SkinType && compareTo((SkinType) other) == 0;
    }

    @Override
    public final int compareTo(SkinType o) {
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public final int hashCode() {
        return name().hashCode();
    }

    public static Collection<SkinType> values() {
        return VALUES.values();
    }

    public static SkinType of(String name) {
        return VALUES.computeIfAbsent(name.toUpperCase(), SkinType::new);
    }

    public static Adapter adapter() {
        return new Adapter();
    }

    static final class Adapter extends TypeAdapter<SkinType> {

        @Override
        public void write(JsonWriter out, SkinType value) throws IOException {
            out.value(value == null ? null : value.name());
        }

        @Override
        public SkinType read(JsonReader in) throws IOException {
            return in.peek() != JsonToken.STRING ? null : SkinType.of(in.nextString());
        }
    }
}
