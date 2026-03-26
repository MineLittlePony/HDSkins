package com.minelittlepony.hdskins.client.gui.player.skins;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins.Posture.SkinVariant;
import com.minelittlepony.hdskins.client.resources.HDPlayerSkinTextureDownloader;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.resources.Identifier;

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

        private Optional<CompletableFuture<Identifier>> local = Optional.empty();

        public LocalTexture(SkinType type, Supplier<Identifier> blank) {
            this.type = type;
            id = HDSkins.id("generated_preview/" + getPosture().getProfile().id().toString() + "/" + type.getPathName());
            defaultTexture = blank;
        }

        @Override
        public Identifier getId() {
            return isReady() ? id : defaultTexture.get();
        }

        public void setLocal(Path file) throws IOException {
            local.ifPresent(l -> l.cancel(true));
            local = Optional.of(HDPlayerSkinTextureDownloader.downloadAndRegisterTexture(id, file, "", type));
        }

        @Override
        public boolean isReady() {
            return local.filter(i -> i.isDone() && !i.isCancelled() && !i.isCompletedExceptionally()).isPresent();
        }

        @Override
        public void close() {
            local.ifPresent(l -> l.cancel(true));
            local = Optional.empty();
        }
    }
}
