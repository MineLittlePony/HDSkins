package com.minelittlepony.hdskins.profile.skin;

import com.google.common.collect.Lists;
import com.minelittlepony.common.util.TextureConverter;

import java.util.List;

public class SkinParsingService {

    private final List<TextureConverter> skinModifiers = Lists.newArrayList();

    public void addModifier(TextureConverter modifier) {
        skinModifiers.add(modifier);
    }

    public void modifySkin(TextureConverter.Drawer drawer) {
        skinModifiers.forEach(converter -> converter.convertTexture(drawer));
    }
}
