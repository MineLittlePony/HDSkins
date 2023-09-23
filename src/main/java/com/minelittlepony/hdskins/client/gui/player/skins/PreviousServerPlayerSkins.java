package com.minelittlepony.hdskins.client.gui.player.skins;

import java.util.function.Supplier;

import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.gui.player.skins.ServerPlayerSkins.Skin;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.util.Identifier;

public class PreviousServerPlayerSkins extends PlayerSkins<ServerPlayerSkins.Skin> {

    private final ServerPlayerSkins.Skin skin;

    public PreviousServerPlayerSkins(ServerPlayerSkins.Skin skin) {
        super(Posture.NULL);
        this.skin = skin;
    }

    @Override
    protected Skin createTexture(SkinType type, Supplier<Identifier> blank) {
        return skin;
    }

    @Override
    protected boolean isProvided(SkinType type) {
        return getPosture().getActiveSkinType() == type;
    }

    @Override
    public String getSkinVariant() {
        return VanillaModels.DEFAULT;
    }
}
