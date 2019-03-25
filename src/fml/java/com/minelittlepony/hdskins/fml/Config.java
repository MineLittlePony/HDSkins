package com.minelittlepony.hdskins.fml;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.minelittlepony.hdskins.HDSkins;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public class Config extends HDSkins {
    static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private final Path configFile;

    Config(Path file) {
        configFile = file;
    }

    @Override
    public Path getAssetsDirectory() {
        return Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.ASSETSDIR.get()).get();
    }

    @Override
    public void saveConfig() {
        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(configFile))) {
            writer.setIndent("    ");

            gson.toJson(this, Config.class, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected <T extends Entity> void addRenderer(Class<T> type, Function<RenderManager, Render<T>> renderer) {
        RenderingRegistry.registerEntityRenderingHandler(type, rm -> renderer.apply(rm));
    }

    static Config of(Path file) {
        Config result = null;

        if (Files.exists(file)) {
            try (BufferedReader s = Files.newBufferedReader(file)) {
                result = gson.fromJson(s, Config.class);
            } catch (IOException ignored) {
                result = null;
            }
        }

        if (result == null) {
            result = new Config(file);
        }

        result.saveConfig();

        return result;
    }
}
