package com.minelittlepony.hdskins.client.resources;

import java.util.function.Supplier;

import com.minelittlepony.hdskins.client.dummy.PlayerSkins;
import com.minelittlepony.hdskins.client.resources.ServerPlayerSkins.Skin;
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
}
