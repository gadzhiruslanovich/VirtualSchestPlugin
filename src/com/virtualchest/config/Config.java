package com.virtualchest.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.virtualchest.storage.ChestStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Config {

    private static final Logger LOGGER = Logger.getLogger(Config.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // singleton instance
    public static Config config;

    // data
    private Inventory inventory = new Inventory();

    public static final class Inventory {
        public HashMap<UUID, ArrayList<ChestStorage.ChestItem>> items = new HashMap<>();
    }

    // обычный конструктор без рекурсии
    public Config() { }

    public static Inventory getInventory() {
        if (config == null) {
            throw new IllegalStateException("Config is not loaded. Call Config.load(path) first.");
        }
        return config.inventory;
    }

    public static Config load(Path configPath) {
        Config loaded = null;

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                loaded = GSON.fromJson(json, Config.class);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load config, using defaults", e);
            }
        }

        if (loaded == null) {
            loaded = new Config();
            LOGGER.info("Created default config at " + configPath);
        }

        // нормализуем поля на случай старых конфигов
        if (loaded.inventory == null) loaded.inventory = new Inventory();
        if (loaded.inventory.items == null) loaded.inventory.items = new HashMap<>();

        // сохраняем и (важно) публикуем как singleton
        loaded.save(configPath);
        config = loaded;

        LOGGER.info("Config loaded from " + configPath);
        return loaded;
    }

    public void save(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            String json = GSON.toJson(this);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save config", e);
        }
    }

    public static String format(String message, Object... args) {
        return String.format(message, args);
    }
}
