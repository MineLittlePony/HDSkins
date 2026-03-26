package com.minelittlepony.hdskins.client;

import java.util.Locale;

import net.minecraft.world.entity.player.PlayerModelType;

public class VanillaModels {
    public static final String SLIM = PlayerModelType.SLIM.getSerializedName();
    public static final String WIDE = PlayerModelType.WIDE.getSerializedName();
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
