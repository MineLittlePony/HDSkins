package com.minelittlepony.hdskins.profile;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

public class SkinType implements Comparable<SkinType> {

    private static final TypeAdapter<SkinType> ADAPTER = new Adapter();
    private static final Map<String, SkinType> VALUES = new HashMap<>();
    private static final Map<MinecraftProfileTexture.Type, SkinType> VANILLA = new EnumMap<>(MinecraftProfileTexture.Type.class);

    public static final SkinType UNKNOWN = register("UNKNOWN");
    public static final SkinType SKIN = forVanilla(MinecraftProfileTexture.Type.SKIN);
    public static final SkinType CAPE = forVanilla(MinecraftProfileTexture.Type.CAPE);
    public static final SkinType ELYTRA = forVanilla(MinecraftProfileTexture.Type.ELYTRA);

    private final String name;

    protected SkinType(String name) {
        this.name = name.toUpperCase();
    }

    public final String name() {
        return name;
    }

    public boolean isKnown() {
        return this != UNKNOWN;
    }

    public Optional<MinecraftProfileTexture.Type> getEnum() {
        return Optional.empty();
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof SkinType && compareTo((SkinType)other) == 0;
    }

    @Override
    public final int compareTo(SkinType o) {
        return name().compareTo(o.name());
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public final int hashCode() {
        return name().hashCode();
    }

    public static TypeAdapter<SkinType> adapter() {
        return ADAPTER;
    }

    public static Collection<SkinType> values() {
        return VALUES.values();
    }

    public static SkinType register(String name) {
        return VALUES.computeIfAbsent(name, SkinType::new);
    }

    public static SkinType forVanilla(MinecraftProfileTexture.Type vanilla) {
        return VANILLA.computeIfAbsent(vanilla, VanillaType::new);
    }

    private static final class VanillaType extends SkinType {
        private final Optional<MinecraftProfileTexture.Type> vanilla;

        VanillaType(MinecraftProfileTexture.Type vanilla) {
            super(vanilla.name());
            this.vanilla = Optional.of(vanilla);
            VALUES.put(name(), this);
        }

        @Override
        public Optional<MinecraftProfileTexture.Type> getEnum() {
            return vanilla;
        }
    }

    private static final class Adapter extends TypeAdapter<SkinType> {

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
