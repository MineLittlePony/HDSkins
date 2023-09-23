package com.minelittlepony.hdskins.client;

import java.util.Locale;

import net.minecraft.client.util.SkinTextures;

public class VanillaModels {
    public static final String SLIM = SkinTextures.Model.SLIM.getName();
    public static final String DEFAULT = SkinTextures.Model.WIDE.getName();

    public static String of(String model) {
        return model == null ? DEFAULT : model;
    }

    public static boolean isSlim(String model) {
        return model != null && model.toLowerCase(Locale.ROOT).contains(SLIM);
    }

    public static boolean isFat(String model) {
        return !isSlim(model);
    }
}
