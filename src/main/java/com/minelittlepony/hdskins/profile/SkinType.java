package com.minelittlepony.hdskins.profile;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.TypeAdapter;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.hdskins.util.Registries;
import com.minelittlepony.hdskins.util.RegistryTypeAdapter;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class SkinType implements Comparable<SkinType> {

    public static final Registry<SkinType> REGISTRY = Registries.createDefaulted(new Identifier("hdskins", "skin_type"), "hdskins:unknown");

    private static final TypeAdapter<SkinType> ADAPTER = new RegistryTypeAdapter<>(REGISTRY);
    private static final Map<MinecraftProfileTexture.Type, SkinType> VANILLA = new EnumMap<>(MinecraftProfileTexture.Type.class);

    public static final SkinType UNKNOWN = register(new Identifier("hdskins", "unknown"), ItemStack.EMPTY);
    public static final SkinType SKIN = forVanilla(MinecraftProfileTexture.Type.SKIN, new ItemStack(Items.LEATHER_CHESTPLATE));
    public static final SkinType CAPE = forVanilla(MinecraftProfileTexture.Type.CAPE, new ItemStack(Items.BARRIER));
    public static final SkinType ELYTRA = forVanilla(MinecraftProfileTexture.Type.ELYTRA, new ItemStack(Items.ELYTRA));

    private final Identifier id;
    private final ItemStack iconStack;

    protected SkinType(Identifier id, ItemStack iconStack) {
        this.id = id;
        this.iconStack = iconStack;
    }

    public String name() {
        return getId().toString();
    }

    public Style getStyle() {
        return new Style()
                .setIcon(iconStack)
                .setTooltip("hdskins.mode." + name().replace(':', '_').replace('/', '_').toLowerCase(), 0, 10);
    }

    public final Identifier getId() {
        return id;
    }

    public final int ordinal() {
        return REGISTRY.getRawId(this);
    }

    public boolean isKnown() {
        return this != UNKNOWN;
    }

    public boolean isVanilla() {
        return getEnum().isPresent();
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
        return getId().compareTo(o.getId());
    }

    @Override
    public String toString() {
        return getId().toString();
    }

    @Override
    public final int hashCode() {
        return getId().hashCode();
    }

    public static TypeAdapter<SkinType> adapter() {
        return ADAPTER;
    }

    public static Stream<SkinType> values() {
        return REGISTRY.stream();
    }

    public static SkinType register(Identifier id, ItemStack iconStack) {
        return Registry.register(REGISTRY, id, new SkinType(id, iconStack));
    }

    public static SkinType forVanilla(MinecraftProfileTexture.Type vanilla) {
        return VANILLA.getOrDefault(vanilla, UNKNOWN);
    }

    public static SkinType forVanilla(MinecraftProfileTexture.Type vanilla, ItemStack iconStack) {
        return VANILLA.computeIfAbsent(vanilla, v -> new VanillaType(vanilla, iconStack));
    }

    private static final class VanillaType extends SkinType {
        private final Optional<MinecraftProfileTexture.Type> vanilla;

        VanillaType(MinecraftProfileTexture.Type vanilla, ItemStack iconStack) {
            super(new Identifier(vanilla.name().toLowerCase(Locale.US)), iconStack);
            this.vanilla = Optional.of(vanilla);
            Registry.register(REGISTRY, getId(), this);
        }

        @Override
        public String name() {
            return vanilla.get().name();
        }

        @Override
        public Optional<MinecraftProfileTexture.Type> getEnum() {
            return vanilla;
        }
    }
}
