package com.minelittlepony.hdskins.profile;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

public final class SkinTypes {

    static final TypeAdapter<SkinType> ADAPTER = new Adapter();

    static final Map<String, SkinType> VALUES = new HashMap<>();

    static final Map<MinecraftProfileTexture.Type, SkinType> VANILLA = new EnumMap<>(MinecraftProfileTexture.Type.class);

    static SkinType forVanilla(MinecraftProfileTexture.Type vanilla) {
        return SkinTypes.VANILLA.computeIfAbsent(vanilla, VanillaSkinTypes::new);
    }

    static class VanillaSkinTypes implements SkinType {
        private final MinecraftProfileTexture.Type vanilla;

        VanillaSkinTypes(MinecraftProfileTexture.Type vanilla) {
            this.vanilla = vanilla;
            VALUES.put(name(), this);
        }

        @Override
        public String name() {
            return vanilla.name();
        }

        @Override
        public MinecraftProfileTexture.Type getEnum() {
            return vanilla;
        }
    }

    static final class Adapter extends TypeAdapter<SkinType> {

        @Override
        public void write(JsonWriter out, SkinType value) throws IOException {
            out.value(value == null ? null : value.name());
        }

        @Override
        public SkinType read(JsonReader in) throws IOException {
            String s = in.nextString();

            if (s == null) {
                return null;
            }

            return VALUES.getOrDefault(s, SkinType.UNKNOWN);
        }
    }
}
