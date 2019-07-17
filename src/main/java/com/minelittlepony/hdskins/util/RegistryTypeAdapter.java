package com.minelittlepony.hdskins.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.io.IOException;

public class RegistryTypeAdapter<T> extends TypeAdapter<T> {

    private final Registry<T> registry;

    public RegistryTypeAdapter(Registry<T> registry) {
        this.registry = registry;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value != null) {
            Identifier id = registry.getId(value);
            if (id != null) {
                out.value(id.toString());
                return;
            }
        }
        out.nullValue();
    }

    @Override
    public T read(JsonReader in) throws IOException {
        return registry.get(new Identifier(in.nextString()));
    }
}
