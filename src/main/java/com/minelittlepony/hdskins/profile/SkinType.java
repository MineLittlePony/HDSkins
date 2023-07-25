package com.minelittlepony.hdskins.profile;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.TypeAdapter;
import com.minelittlepony.common.client.gui.sprite.ItemStackSprite;
import com.minelittlepony.common.client.gui.sprite.TextureSprite;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.common.util.registry.RegistryTypeAdapter;
import com.minelittlepony.common.util.registry.Registries;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.text.Text;

public class SkinType implements Comparable<SkinType> {

    public static final SkinType UNKNOWN = new SkinType(new Identifier("hdskins", "unknown"), ItemStack.EMPTY);

    public static final Registry<SkinType> REGISTRY = Registries.createDefaulted(new Identifier("hdskins", "skin_type"), SkinType::getId, UNKNOWN);

    private static final TypeAdapter<SkinType> ADAPTER = RegistryTypeAdapter.of(REGISTRY, (ls, registry) -> {
        return registry.stream().filter(type -> type.getParameterizedName().equals(ls)).findFirst().orElseGet(() -> createUnsupported(ls));
    });
    private static final Map<MinecraftProfileTexture.Type, SkinType> VANILLA = new EnumMap<>(MinecraftProfileTexture.Type.class);

    public static final SkinType SKIN = forVanilla(MinecraftProfileTexture.Type.SKIN, new ItemStack(Items.LEATHER_CHESTPLATE));
    public static final SkinType CAPE = forVanilla(MinecraftProfileTexture.Type.CAPE, new ItemStack(Items.BARRIER));
    public static final SkinType ELYTRA = forVanilla(MinecraftProfileTexture.Type.ELYTRA, new ItemStack(Items.ELYTRA));

    private final Identifier id;
    private final ItemStack iconStack;
    private final Identifier icon;

    protected SkinType(Identifier id, ItemStack iconStack) {
        this.id = id;
        this.icon = new Identifier(getId().getNamespace(), "textures/gui/skin_type/" + getId().getPath() + ".png");
        this.iconStack = iconStack;
    }

    public Identifier icon() {
        return icon;
    }

    public String name() {
        return getId().toString();
    }

    public String getParameterizedName() {
        return name().replace(':', '_').replace('/', '_').toLowerCase(Locale.US);
    }

    public String getPathName() {
        return getId().getNamespace() + "/" + getId().getPath();
    }

    public Style getStyle() {

        if (iconStack.getItem() == Items.BARRIER) {
            return new Style()
                    .setIcon(new TextureSprite()
                            .setTexture(UNKNOWN.icon())
                            .setPosition(2, 2)
                            .setSize(16, 16)
                            .setTextureSize(16, 16))
                    .setText(Text.translatable("skin_type.hdskins.unknown", getId().toString()))
                    .setTooltip(getId().toString(), 0, 10);
        }

        return new Style()
                .setIcon(MinecraftClient.getInstance().getResourceManager().getResource(icon).isEmpty()
                        ? new ItemStackSprite().setStack(iconStack)
                        : new TextureSprite().setTexture(icon).setPosition(2, 2).setSize(16, 16).setTextureSize(16, 16))
                .setText(Text.translatable("hdskins.skin_type", Text.translatable(Util.createTranslationKey("skin_type", getId()))))
                .setTooltip(getId().toString(), 0, 10);
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

    private static SkinType createUnsupported(String parameterizedName) {
        return register(deParameterize(parameterizedName), Items.BARRIER.getDefaultStack());
    }

    private static Identifier deParameterize(String parameterizedName) {
        String[] parts = parameterizedName.split("_", 2);
        parts[1] = parts[1].replace('_', '/');
        return new Identifier(parts[0], parts[1]);
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
