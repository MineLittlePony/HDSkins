package com.minelittlepony.hdskins.client.gui;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.resources.Identifier;

public interface RenderStateKeys {
    RenderStateDataKey<Identifier> PANORAMA_TEXTURE_KEY = RenderStateDataKey.create(() -> "HD Skins/Panorama Texture");
}
