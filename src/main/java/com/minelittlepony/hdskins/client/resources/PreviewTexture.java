package com.minelittlepony.hdskins.client.resources;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.annotation.Nullable;

import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.skins.SkinType;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.util.Identifier;

public abstract class PreviewTexture extends HDPlayerSkinTexture {

    private final String model;

    private final String fileUrl;

    public static PreviewTexture create(MinecraftProfileTexture texture, SkinType type, Identifier fallback, @Nullable Runnable callack) {
        boolean[] uploaded = new boolean[1];
        return new PreviewTexture(texture, type, fallback, () -> {
            uploaded[0] = true;
            if (callack != null) {
                callack.run();
            }
        }) {
            @Override
            public boolean isTextureUploaded() {
                return uploaded[0] && getGlId() > -1;
            }

            @Override
            public void clearGlId() {
                super.clearGlId();
                uploaded[0] = true;
            }
        };
    }

    public PreviewTexture(MinecraftProfileTexture texture, SkinType type, Identifier fallback, @Nullable Runnable callack) {
        super(tempFile(texture.getHash()), texture.getUrl(), type, fallback, callack);

        this.model = VanillaModels.of(texture.getMetadata("model"));
        this.fileUrl = texture.getUrl();
    }

    public abstract boolean isTextureUploaded();

    public String getUrl() {
        return fileUrl;
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
