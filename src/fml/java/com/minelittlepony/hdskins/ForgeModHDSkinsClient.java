package com.minelittlepony.hdskins;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.loading.FMLPaths;

import java.util.function.Function;

class ForgeModHDSkinsClient implements IModUtilities {

    public ForgeModHDSkinsClient() {
        HDSkins hdskins = new HDSkins(this, FMLPaths.CONFIGDIR.get().resolve("hdskins.json"));
        hdskins.postinit();
    }

    @Override
    public <T extends Entity> void addRenderer(Class<T> type, Function<RenderManager, Render<T>> renderer) {
        RenderingRegistry.registerEntityRenderingHandler(type, renderer::apply);
    }
}