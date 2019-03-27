package com.minelittlepony.hdskins;

import com.google.gson.GsonBuilder;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.net.SkinServer;
import com.minelittlepony.hdskins.net.SkinServerSerializer;
import com.mumfrey.liteloader.Configurable;
import com.mumfrey.liteloader.InitCompleteListener;
import com.mumfrey.liteloader.ViewportListener;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.modconfig.AdvancedExposable;
import com.mumfrey.liteloader.modconfig.ConfigPanel;
import com.mumfrey.liteloader.modconfig.ConfigStrategy;
import com.mumfrey.liteloader.modconfig.ExposableOptions;
import com.mumfrey.liteloader.util.ModUtilities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

public class LiteModHDSkins implements IModUtilities, InitCompleteListener {

    private final HDSkins hdskins = new HDSkins(this);

    @Override
    public String getName() {
        return HDSkins.MOD_NAME;
    }

    @Override
    public String getVersion() {
        return HDSkins.VERSION;
    }

    @Override
    public void init(File configPath) {
        Config config = new Config();
        LiteLoader.getInstance().registerExposable(config, null);

        hdskins.init(config);
    }

    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath) {
        hdskins.clearSkinCache();
    }

    @Override
    public File getConfigFile(File configFile, File configFileLocation, String defaultFileName) {
        return null;
    }

    @Override
    public void onInitCompleted(Minecraft minecraft, LiteLoader loader) {
        hdskins.posInit();
    }

    @Override
    public <T extends Entity> void addRenderer(Class<T> type, Function<RenderManager, Render<T>> renderer) {
        ModUtilities.addRenderer(type, renderer.apply(Minecraft.getInstance().getRenderManager()));
    }

    @Override
    public Path getAssetsDirectory() {
        return Paths.get(LiteLoader.getAssetsDirectory().toURI());
    }

    @ExposableOptions(strategy = ConfigStrategy.Unversioned, filename = "hdskins")
    class Config extends AbstractConfig implements AdvancedExposable {

        @Override
        public void save() {
            LiteLoader.getInstance().writeConfig(this);
        }

        @Override
        public void setupGsonSerialiser(GsonBuilder gsonBuilder) {
            gsonBuilder.registerTypeAdapter(SkinServer.class, new SkinServerSerializer());
        }
    }
}
