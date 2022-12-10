package com.minelittlepony.hdskins.client.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;

import com.minelittlepony.hdskins.client.dummy.PlayerSkins;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

public class LocalPlayerSkins extends PlayerSkins<LocalPlayerSkins.LocalTexture> {

    private boolean previewThinArms = false;

    public LocalPlayerSkins(Posture posture) {
        super(posture);
    }

    @Override
    protected LocalTexture createTexture(SkinType type, Supplier<Identifier> blank) {
        return new LocalTexture(type, blank);
    }

    public void setPreviewThinArms(boolean thinArms) {
        previewThinArms = thinArms;
    }

    @Override
    public boolean usesThinSkin() {
        return previewThinArms;
    }

    @Override
    protected boolean isProvided(SkinType type) {
        return getPosture().getActiveSkinType() == type;
    }

    public class LocalTexture implements PlayerSkins.PlayerSkin {
        private final Identifier id;
        private final Supplier<Identifier> defaultTexture;

        private Optional<Texture.MemoryTexture> local = Optional.empty();

        public LocalTexture(SkinType type, Supplier<Identifier> blank) {
            id = new Identifier("hdskins", "generated_preview/" + posture.getProfile().getId().toString() + "/" + type.getPathName());
            defaultTexture = blank;
        }

        @Override
        public Identifier getId() {
            return isReady() ? id : defaultTexture.get();
        }

        public void setLocal(Path file) throws IOException {
            local.ifPresent(AbstractTexture::close);

            try (InputStream input = Files.newInputStream(file)) {
                Texture.MemoryTexture image = new Texture.MemoryTexture(HDPlayerSkinTexture.filterPlayerSkins(NativeImage.read(input), TextureLoader.Exclusion.NULL), id);
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, image);
                local = Optional.of(image);
            }
        }

        @Override
        public boolean isReady() {
            return local.filter(Texture::isLoaded).isPresent();
        }

        @Override
        public void close() {
            local.ifPresent(AbstractTexture::close);
            local = Optional.empty();
        }
    }
}
