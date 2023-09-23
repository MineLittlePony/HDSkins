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

    String getModel(String fallback);

    default SkinTextures toSkinTextures() {
        return new SkinTextures(
                getSkin(SkinType.SKIN).orElse(null),
                null,
                getSkin(SkinType.CAPE).orElse(null),
                getSkin(SkinType.ELYTRA).orElse(null),
                VanillaModels.isSlim(getModel(VanillaModels.DEFAULT)) ? Model.SLIM : Model.WIDE,
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
            public String getModel(String fallback) {
                return supplier.get().model().getName();
            }
        };
    }

    static DynamicSkinTextures union(Supplier<? extends DynamicSkinTextures> a, DynamicSkinTextures b) {
        return new DynamicSkinTextures() {
            @Override
            public Set<Identifier> getProvidedSkinTypes() {
                return Stream.concat(
                        a.get().getProvidedSkinTypes().stream(),
                        b.getProvidedSkinTypes().stream()
                ).distinct().collect(Collectors.toSet());
            }

            @Override
            public Optional<Identifier> getSkin(SkinType type) {
                return Optional.ofNullable(a.get().getSkin(type, b));
            }

            @Override
            public String getModel(String fallback) {
                return a.get().getModel(b.getModel(fallback));
            }
        };
    }
}
