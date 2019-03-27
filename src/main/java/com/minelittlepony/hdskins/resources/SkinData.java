package com.minelittlepony.hdskins.resources;

import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.UUID;

import com.google.gson.annotations.Expose;

class SkinData {
    @Expose
    List<Skin> skins;
}

class Skin {
    @Expose
    String name;

    @Expose
    UUID uuid;

    @Expose
    String skin;

    public ResourceLocation getTexture() {
        return new ResourceLocation("hdskins", String.format("textures/skins/%s.png", skin));
    }
}
