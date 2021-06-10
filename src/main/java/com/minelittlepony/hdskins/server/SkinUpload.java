package com.minelittlepony.hdskins.server;

import net.minecraft.client.util.Session;

import org.jetbrains.annotations.Nullable;
import com.minelittlepony.hdskins.profile.SkinType;

import java.net.URI;
import java.util.Map;

public record SkinUpload(
        Session session,
        SkinType type,
        @Nullable URI image,
        Map<String, String> metadata
) {

    public String getSchemaAction() {
        return image == null ? "none" : image.getScheme();
    }
}
