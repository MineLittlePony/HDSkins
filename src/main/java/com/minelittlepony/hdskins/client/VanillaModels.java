package com.minelittlepony.hdskins.client;

import java.util.Locale;

import net.minecraft.entity.player.PlayerSkinType;

public class VanillaModels {
    public static final String SLIM = PlayerSkinType.SLIM.asString();
    public static final String WIDE = PlayerSkinType.WIDE.asString();
    public static final String DEFAULT = "default";

    public static String of(String model) {
        return model == null ? WIDE : model;
    }

    public static boolean isSlim(String model) {
        return model != null && model.toLowerCase(Locale.ROOT).contains(SLIM);
    }

    public static boolean isFat(String model) {
        return !isSlim(model);
    }
}
