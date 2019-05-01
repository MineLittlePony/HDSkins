package com.minelittlepony.hdskins;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.Function;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;

class ForgeModHDSkinsClient implements IModUtilities {

    public ForgeModHDSkinsClient() {
        HDSkins hdskins = new HDSkins(this, FMLPaths.CONFIGDIR.get().resolve("hdskins.json"));
        hdskins.postinit();
    }

    @Override
    public <T extends Entity> void addRenderer(Class<T> type, Function<RenderManager, Render<T>> renderer) {
        RenderingRegistry.registerEntityRenderingHandler(type, renderer::apply);
    }

    @Override
    public Path getAssetsDirectory() {
        return Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.ASSETSDIR.get()).get();
    }
}