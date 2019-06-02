package com.minelittlepony.hdskins;

import javax.annotation.Nullable;

import com.minelittlepony.common.client.IModUtilities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.MinecraftClient;

public class FabMod implements ClientModInitializer, ClientTickCallback, IModUtilities {

    @Nullable
    private HDSkins hd;

    private boolean firstTick = true;

    @Override
    public void onInitializeClient() {
        ClientTickCallback.EVENT.register(this);

        hd = new HDSkins(this);
    }

    @Override
    public void tick(MinecraftClient client) {
        if (hd == null) {
            return;
        }

        if (firstTick) {
            firstTick = false;

            hd.postInit(client);
        }
    }
}
