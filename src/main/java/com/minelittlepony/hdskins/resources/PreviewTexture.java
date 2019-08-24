package com.minelittlepony.hdskins.resources;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.annotation.Nullable;

import com.minelittlepony.hdskins.VanillaModels;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.texture.ImageFilter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.util.Identifier;

public class PreviewTexture extends PlayerSkinTexture {

    private boolean uploaded;

    private final String model;

    private final String fileUrl;

    public PreviewTexture(MinecraftProfileTexture texture, Identifier fallbackTexture, @Nullable ImageFilter imageBuffer) {
        // Always provided a cache file to force skin textures to load correctly.
        //  Mojang closes the http connection before MinecraftClient.getInstance().execute(..) comes back
        //  if we don't provide a file location, it will attempt to read from the http connection's input stream
        //  which will have been closed before prior to their callback executing.
        super(tempFile(texture.getHash()), texture.getUrl(), fallbackTexture, imageBuffer);

        this.model = VanillaModels.of(texture.getMetadata("model"));
        this.fileUrl = texture.getUrl();
    }

    @Override
    public void method_4534(NativeImage nativeImage_1) {
        super.method_4534(nativeImage_1);
        uploaded = true;
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

    @Nullable
    private static File tempFile(String filename) {
        try {
            File f = Files.createTempFile(filename, "skin-preview").toFile();
            f.delete();
            return f;
        } catch (IOException ignored) {}
        return null;
    }
}
