package com.minelittlepony.hdskins.client.profile;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.SkinTextures.Model;
import net.minecraft.util.Identifier;

public interface DynamicSkinTextures {
    Function<SkinTextures, Identifier> NIL = t -> null;
    Map<SkinType, Function<SkinTextures, Identifier>> TEXTURE_LOOKUP = Map.of(
            SkinType.SKIN, SkinTextures::texture,
            SkinType.CAPE, SkinTextures::capeTexture,
            SkinType.ELYTRA, SkinTextures::elytraTexture
    );

    Set<Identifier> getProvidedSkinTypes();

    Optional<Identifier> getSkin(SkinType type);

    default Identifier getSkin(SkinType type, DynamicSkinTextures fallback) {
        return getSkin(type).orElseGet(() -> fallback.getSkin(type).orElse(null));
    }

    Optional<String> getModel();

    boolean hasChanged();

    static SkinTextures toSkinTextures(DynamicSkinTextures dynamic) {
        return new SkinTextures(
            dynamic.getSkin(SkinType.SKIN).orElse(null),
            null,
            dynamic.getSkin(SkinType.CAPE).orElse(null),
            dynamic.getSkin(SkinType.ELYTRA).orElse(null),
            VanillaModels.isSlim(dynamic.getModel().orElse(VanillaModels.DEFAULT)) ? Model.SLIM : Model.WIDE,
            false
        );
    }

    static DynamicSkinTextures of(Supplier<SkinTextures> supplier) {
        return new DynamicSkinTextures() {
            @Override
            public Set<Identifier> getProvidedSkinTypes() {
                return TEXTURE_LOOKUP.keySet().stream().filter(type -> getSkin(type).isEmpty()).map(SkinType::getId).collect(Collectors.toSet());
            }

            @Override
            public Optional<Identifier> getSkin(SkinType type) {
                return Optional.ofNullable(TEXTURE_LOOKUP.getOrDefault(type, NIL).apply(supplier.get()));
            }

            @Override
            public Optional<String> getModel() {
                return getSkin(SkinType.SKIN).isPresent() ? Optional.ofNullable(supplier.get().model().getName()) : Optional.empty();
            }

            @Override
            public boolean hasChanged() {
                return false;
            }
        };
    }

    default DynamicSkinTextures union(DynamicSkinTextures b) {
        final DynamicSkinTextures a = this;
        return new DynamicSkinTextures() {
            @Override
            public Set<Identifier> getProvidedSkinTypes() {
                return Stream.concat(
                        a.getProvidedSkinTypes().stream(),
                        b.getProvidedSkinTypes().stream()
                ).distinct().collect(Collectors.toSet());
            }

            @Override
            public Optional<Identifier> getSkin(SkinType type) {
                return Optional.ofNullable(a.getSkin(type, b));
            }

            @Override
            public Optional<String> getModel() {
                return b.getModel().or(a::getModel);
            }

            @Override
            public boolean hasChanged() {
                return a.hasChanged() || b.hasChanged();
            }
        };
    }
}
