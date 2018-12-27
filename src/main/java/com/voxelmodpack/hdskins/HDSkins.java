package com.voxelmodpack.hdskins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voxelmodpack.hdskins.gui.EntityPlayerModel;
import com.voxelmodpack.hdskins.gui.RenderPlayerModel;
import com.voxelmodpack.hdskins.server.SkinServer;
import com.voxelmodpack.hdskins.server.SkinServerSerializer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;

@Mod(modid = "hdskins")
public class HDSkins {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(SkinServer.class, new SkinServerSerializer())
            .create();

    @Mod.Instance
    public static HDSkins instance;

    public Logger logger;

    private File configFile;
    private Config config;

    private HDSkinManager skinManager;

    @Mod.EventHandler
    public void init(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        configFile = new File(event.getModConfigurationDirectory(), "hdskins.json");

        loadConfig();

        skinManager = new HDSkinManager();

        RenderingRegistry.registerEntityRenderingHandler(EntityPlayerModel.class, RenderPlayerModel::new);
    }

    @Mod.EventHandler
    public void postinit(FMLPostInitializationEvent event) {
        for (SkinServer server : config.skinServers) {
            skinManager.addSkinServer(server);
        }
    }

    public HDSkinManager getSkinManager() {
        return skinManager;
    }

    public Config getConfig() {
        return config;
    }

    private void loadConfig() {
        try (Reader reader = Files.newBufferedReader(configFile.toPath())) {
            config = gson.fromJson(reader, Config.class);
        } catch (IOException e) {
            logger.error("Failed to load config. Loading defaults", e);
            config = new Config();

            // save the default config if it failed or file does not exist.
            saveConfig();

        }
    }

    public void saveConfig() {
        try (Writer writer = Files.newBufferedWriter(configFile.toPath())) {
            gson.toJson(config, writer);
        } catch (IOException e1) {
            logger.error("Failed to save default config.", e1);
        }
    }
}
