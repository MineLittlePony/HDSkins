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

import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;


public interface DynamicSkinTextures {
    Function<PlayerSkin, ClientAsset.Texture> NIL = _ -> null;
    Map<SkinType, Function<PlayerSkin, ClientAsset.Texture>> TEXTURE_LOOKUP = Map.of(
            SkinType.SKIN, PlayerSkin::body,
            SkinType.CAPE, PlayerSkin::cape,
            SkinType.ELYTRA, PlayerSkin::elytra
    );

    Set<Identifier> getProvidedSkinTypes();

    Optional<ClientAsset.Texture> getSkin(SkinType type);

    default ClientAsset.Texture getSkin(SkinType type, DynamicSkinTextures fallback) {
        return getSkin(type).orElseGet(() -> fallback.getSkin(type).orElse(null));
    }

    Optional<String> getModel();

    boolean isNewer(long age);

    static PlayerSkin toSkinTextures(DynamicSkinTextures dynamic) {
        return new PlayerSkin(
            dynamic.getSkin(SkinType.SKIN).orElse(null),
            dynamic.getSkin(SkinType.CAPE).orElse(null),
            dynamic.getSkin(SkinType.ELYTRA).orElse(null),
            VanillaModels.isSlim(dynamic.getModel().orElse(VanillaModels.DEFAULT)) ? PlayerModelType.SLIM : PlayerModelType.WIDE,
            false
        );
    }

    static DynamicSkinTextures of(Supplier<PlayerSkin> supplier) {
        return new DynamicSkinTextures() {
            @Override
            public Set<Identifier> getProvidedSkinTypes() {
                return TEXTURE_LOOKUP.keySet().stream().filter(type -> getSkin(type).isEmpty()).map(SkinType::getId).collect(Collectors.toSet());
            }

            @Override
            public Optional<ClientAsset.Texture> getSkin(SkinType type) {
                return Optional.ofNullable(TEXTURE_LOOKUP.getOrDefault(type, NIL).apply(supplier.get()));
            }

            @Override
            public Optional<String> getModel() {
                return getSkin(SkinType.SKIN).isPresent() ? Optional.ofNullable(supplier.get().model().name()) : Optional.empty();
            }

            @Override
            public boolean isNewer(long age) {
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
            public Optional<ClientAsset.Texture> getSkin(SkinType type) {
                return Optional.ofNullable(a.getSkin(type, b));
            }

            @Override
            public Optional<String> getModel() {
                return b.getModel().or(a::getModel);
            }

            @Override
            public boolean isNewer(long age) {
                return a.isNewer(age) || b.isNewer(age);
            }
        };
    }
}
