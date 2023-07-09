package com.minelittlepony.hdskins.client;

import java.util.Locale;

public class VanillaModels {
    public static final String SLIM = "slim";
    public static final String DEFAULT = "default";

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
