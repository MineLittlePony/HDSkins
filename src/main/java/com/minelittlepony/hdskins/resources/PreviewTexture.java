package com.minelittlepony.hdskins.resources;

import com.minelittlepony.hdskins.VanillaModels;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.texture.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class PreviewTexture extends ThreadDownloadImageData {

    private boolean uploaded;

    private final String model;

    private final String fileUrl;

    public PreviewTexture(MinecraftProfileTexture texture, ResourceLocation fallbackTexture, @Nullable IImageBuffer imageBuffer) {
        super(null, texture.getUrl(), fallbackTexture, imageBuffer);

        this.model = VanillaModels.nonNull(texture.getMetadata("model"));
        this.fileUrl = texture.getUrl();
    }

    public boolean isTextureUploaded() {
        return uploaded && getGlTextureId() > -1;
    }

    public String getUrl() {
        return fileUrl;
    }

    @Override
    public void deleteGlTexture() {
        super.deleteGlTexture();
        uploaded = true;
    }

    public boolean hasModel() {
        return model != null;
    }

    public boolean usesThinArms() {
        return VanillaModels.isSlim(model);
    }
}
