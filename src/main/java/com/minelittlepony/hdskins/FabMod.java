package com.minelittlepony.hdskins;

import java.nio.file.Path;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.minelittlepony.hdskins.mixin.MixinEntityRenderDispatcher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;

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

    @Override
    public <T extends Entity> void addRenderer(Class<T> type, Function<EntityRenderDispatcher, EntityRenderer<T>> renderer) {
        EntityRenderDispatcher mx = MinecraftClient.getInstance().getEntityRenderManager();
        ((MixinEntityRenderDispatcher)mx).getRenderers().put(type, renderer.apply(mx));
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDirectory().toPath();
    }

    @Override
    public Path getAssetsDirectory() {
        return FabricLoader.getInstance().getGameDirectory().toPath().resolve("assets");
    }
}
