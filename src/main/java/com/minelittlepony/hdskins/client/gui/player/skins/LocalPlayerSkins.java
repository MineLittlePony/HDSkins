package com.minelittlepony.hdskins.client.gui.player.skins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;

import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins.Posture.SkinVariant;
import com.minelittlepony.hdskins.client.resources.HDPlayerSkinTexture;
import com.minelittlepony.hdskins.client.resources.Texture;
import com.minelittlepony.hdskins.client.resources.TextureLoader;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

public class LocalPlayerSkins extends PlayerSkins<LocalPlayerSkins.LocalTexture> {

    public LocalPlayerSkins(Posture posture) {
        super(posture);
    }

    @Override
    protected LocalTexture createTexture(SkinType type, Supplier<Identifier> blank) {
        return new LocalTexture(type, blank);
    }

    @Override
    public String getSkinVariant() {
        return getPosture().getSkinVariant().map(SkinVariant::name).orElse(VanillaModels.DEFAULT);
    }

    @Override
    protected boolean isProvided(SkinType type) {
        return getPosture().getActiveSkinType() == type;
    }

    public class LocalTexture implements PlayerSkins.PlayerSkin {
        private final SkinType type;
        private final Identifier id;
        private final Supplier<Identifier> defaultTexture;

        private Optional<Texture.MemoryTexture> local = Optional.empty();

        public LocalTexture(SkinType type, Supplier<Identifier> blank) {
            this.type = type;
            id = new Identifier("hdskins", "generated_preview/" + getPosture().getProfile().getId().toString() + "/" + type.getPathName());
            defaultTexture = blank;
        }

        @Override
        public Identifier getId() {
            return isReady() ? id : defaultTexture.get();
        }

        public void setLocal(Path file) throws IOException {
            local.ifPresent(AbstractTexture::close);

            try (InputStream input = Files.newInputStream(file)) {
                Texture.MemoryTexture image = new Texture.MemoryTexture(
                        type != SkinType.SKIN
                            ? NativeImage.read(input)
                            : HDPlayerSkinTexture.filterPlayerSkins(NativeImage.read(input), TextureLoader.Exclusion.NULL), id);
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
