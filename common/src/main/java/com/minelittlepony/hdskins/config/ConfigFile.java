package com.minelittlepony.hdskins.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class ConfigFile<T> {

    private static final Logger logger = LogManager.getLogger();

    private final Path file;
    private final Gson gson;
    private final Type type;
    private final Supplier<T> defaultValue;

    private T config;

    private ConfigFile(Builder<T> builder) {
        this.file = Paths.get("config").resolve(builder.path);
        this.gson = builder.gson.create();
        this.type = builder.type;
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
            config = gson.fromJson(r, type);
        } catch (NoSuchFileException e) {
            config = defaultValue.get();
            save();
        } catch (IOException e) {
            logger.warn("Failed to load config. Using defaults.", e);
            config = defaultValue.get();
        }
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(file)) {
                gson.toJson(config, type, w);
            }
        } catch (IOException e) {
            logger.warn("Failed to save config.", e);
        }
    }

    public static class Builder<T> {

        private Path path;
        private Type type;
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

        public Builder<T> withGson(Consumer<GsonBuilder> gson) {
            gson.accept(this.gson);
            return this;
        }

        public Builder<T> withDefault(Supplier<T> def) {
            this.defaultValue = def;
            return this;
        }

        public ConfigFile<T> build() {
            checkNotNull(path);
            checkNotNull(type);
            return new ConfigFile<>(this);
        }
    }
}
