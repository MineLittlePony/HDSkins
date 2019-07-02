package com.minelittlepony.hdskins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.minelittlepony.hdskins.net.SkinServer;
import com.minelittlepony.hdskins.net.SkinServerSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config extends AbstractConfig {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(SkinServer.class, SkinServerSerializer.instance)
            .create();

    private Path configFile;

    private Config(Path file) {
        configFile = file;
    }

    @Override
    public void save() {
        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(configFile))) {
            writer.setIndent("    ");

            gson.toJson(this, Config.class, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Config of(Path file) {
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
        } else {
            result.configFile = file;
        }

        result.save();

        return result;
    }
}
