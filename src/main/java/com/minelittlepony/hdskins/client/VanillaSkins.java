package com.minelittlepony.hdskins.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class VanillaSkins {
    public static final Identifier NO_SKIN_STEVE = new Identifier("hdskins", "textures/mob/noskin.png");
    public static final Identifier NO_SKIN_ALEX = new Identifier("hdskins", "textures/mob/noskin_alex.png");
    public static final Identifier NO_SKIN_CAPE = new Identifier("hdskins", "textures/mob/noskin_cape.png");

    public static final Map<SkinType, Identifier> NO_TEXTURES = Util.make(new HashMap<>(), map -> {
        map.put(SkinType.SKIN, NO_SKIN_STEVE);
        map.put(SkinType.CAPE, NO_SKIN_CAPE);
        map.put(SkinType.ELYTRA, new Identifier("textures/entity/elytra.png"));
    });

    public static final Map<SkinType, Identifier> NO_TEXTURES_ALEX = Util.make(new HashMap<>(), map -> {
        map.put(SkinType.SKIN, NO_SKIN_ALEX);
    });

    private static final Map<SkinTextures, Identifier> TEXTURE_CONVERSION = new HashMap<>();

    public static Identifier getDefaultTexture(SkinType type, String variant) {
        if (VanillaModels.isSlim(variant) && NO_TEXTURES_ALEX.containsKey(type)) {
            return NO_TEXTURES_ALEX.get(type);
        }
        return NO_TEXTURES.getOrDefault(type, NO_SKIN_STEVE);
    }

    public static Identifier getSkinTextures(UUID profileId, String variant) {
        return TEXTURE_CONVERSION.computeIfAbsent(DefaultSkinHelper.getTexture(profileId), skin -> {
            boolean slimArms = VanillaModels.isSlim(variant);
            return skin.texture().withPath(path -> path.replace(slimArms ? "/wide/" : "/slim/", slimArms ? "/slim/" : "/wide/"));
        });
    }
}
