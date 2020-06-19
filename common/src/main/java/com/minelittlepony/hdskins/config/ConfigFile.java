package com.minelittlepony.hdskins.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;

public class ConfigFile<T> {

    private static final Logger logger = LogManager.getLogger();

    private final Path file;
    private final Type type;
    private final int version;
    private final Gson gson;

    private final Supplier<T> defaultValue;

    private T config;

    private ConfigFile(Builder<T> builder) {
        this.file = Paths.get("config").resolve(builder.path);
        this.type = builder.type;
        this.version = builder.version;
        this.gson = builder.gson.create();
        this.defaultValue = builder.defaultValue;
    }

    public T get() {
        checkState(config != null, "Config has not been loaded");
        return config;
    }

    public void with(Consumer<T> config) {
        config.accept(get());
        save();
    }

    public void load() {
        try (BufferedReader r = Files.newBufferedReader(file)) {
            JsonObject json = gson.fromJson(r, JsonObject.class);

            // check the config version and back up the old one if it changed
            if (checkVersion(json)) {
                config = gson.fromJson(json, type);
            }
        } catch (NoSuchFileException e) {
            config = defaultValue.get();
            save();
        } catch (JsonSyntaxException e) {
            resetConfig("Syntax error", "bad-syntax");
        } catch (IOException e) {
            logger.warn("Failed to load config. Using defaults.", e);
            config = defaultValue.get();
        }
    }

    private boolean checkVersion(JsonObject json) {
        int version = 0;
        if (json.has("version")) {
            version = json.remove("version").getAsInt();
        }
        if (version != this.version) {
            resetConfig("config version changed", String.format("v%d", version));
            return false;
        }
        return true;
    }

    private void resetConfig(String error, String suffix) {
        String filename = file.getFileName().toString();
        String name = FilenameUtils.getBaseName(filename);
        String ext = FilenameUtils.getExtension(filename);
        String oldConfigName = String.format("%s-bak-%s.%s", name, suffix, ext);

        logger.warn("{}: {}. Resetting and backing up as {}.", filename, error, oldConfigName);

        try {
            Path oldConfig = file.resolveSibling(oldConfigName);
            Files.copy(file, oldConfig, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to back up config to " + file, e);
        }

        config = defaultValue.get();
        save();
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            JsonObject json = gson.toJsonTree(config, type).getAsJsonObject();
            json.addProperty("version", version);

            try (BufferedWriter w = Files.newBufferedWriter(file)) {
                gson.toJson(json, w);
            }
        } catch (IOException e) {
            logger.warn("Failed to save config.", e);
        }
    }

    public static class Builder<T> {

        private Path path;
        private Type type;
        private int version = -1;
        private final GsonBuilder gson = new GsonBuilder().setPrettyPrinting();
        public Supplier<T> defaultValue;

        public Builder<T> withPath(Path path) {
            this.path = path;
            return this;
        }

        public Builder<T> withType(Type type) {
            this.type = type;
            return this;
        }

        public Builder<T> withVersion(int version) {
            this.version = version;
            return this;
        }

        public Builder<T> withGson(Consumer<GsonBuilder> gson) {
            gson.accept(this.gson);
            return this;
        }

        public Builder<T> withDefault(Supplier<T> def) {
            this.defaultValue = def;
            return this;
        }

        public ConfigFile<T> build() {
            checkState(path != null);
            checkState(type != null);
            checkState(version >= 0);
            checkState(defaultValue != null);
            return new ConfigFile<>(this);
        }
    }
}
