package com.minelittlepony.hdskins.profile;

import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;

public class EtagProfileTexture extends MinecraftProfileTexture {

    @Nullable
    private final String eTag;

    public EtagProfileTexture(String url, @Nullable String etag, @Nullable Map<String, String> metadata) {
        super(url, metadata);
        eTag = etag;
    }

    @Nullable
    public String getEtag() {
        return eTag;
    }

    @Override
    public String getHash() {
        if (eTag == null) {
            return super.getHash();
        }
        return super.getHash() + eTag;
    }
}
