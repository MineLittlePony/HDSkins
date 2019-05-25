package com.minelittlepony.hdskins.resources;

import com.minelittlepony.hdskins.VanillaModels;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.texture.ImageFilter;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;

public class PreviewTexture extends PlayerSkinTexture {

    private boolean uploaded;

    private final String model;

    private final String fileUrl;

    public PreviewTexture(MinecraftProfileTexture texture, Identifier fallbackTexture, @Nullable ImageFilter imageBuffer) {
        super(null, texture.getUrl(), fallbackTexture, imageBuffer);

        this.model = VanillaModels.nonNull(texture.getMetadata("model"));
        this.fileUrl = texture.getUrl();
    }

    public boolean isTextureUploaded() {
        return uploaded && getGlId() > -1;
    }

    public String getUrl() {
        return fileUrl;
    }

    @Override
    public void clearGlId() {
        super.clearGlId();
        uploaded = true;
    }

    public boolean hasModel() {
        return model != null;
    }

    public boolean usesThinArms() {
        return VanillaModels.isSlim(model);
    }
}
