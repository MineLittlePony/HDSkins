package com.minelittlepony.hdskins;

import java.nio.file.Path;
import java.util.function.Function;

import com.minelittlepony.hdskins.Config;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.IModUtilities;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(HDSkins.MOD_ID)
public class ForgeModHDSkins implements IModUtilities {

    private HDSkins hdskins = new HDSkins(this);

    public ForgeModHDSkins() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientInit);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonInit(final FMLCommonSetupEvent event) {
        hdskins.init(Config.of(FMLPaths.CONFIGDIR.get().resolve("hdskins.json")));
    }

    private void clientInit(FMLClientSetupEvent event) {
        hdskins.posInit();
    }

    @Override
    public Path getAssetsDirectory() {
        return Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.ASSETSDIR.get()).get();
    }

    @Override
    public <T extends Entity> void addRenderer(Class<T> type, Function<RenderManager, Render<T>> renderer) {
        RenderingRegistry.registerEntityRenderingHandler(type, rm -> renderer.apply(rm));
    }
}
