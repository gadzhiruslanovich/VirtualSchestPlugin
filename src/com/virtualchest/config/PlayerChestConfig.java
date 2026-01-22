package com.virtualchest.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.virtualchest.storage.ChestStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerChestConfig {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public UUID uuid;
    public List<ChestStorage.ChestItem> items = new ArrayList<>();

    private static Path rootDir = Path.of("mods/VirtualChest");

    public static PlayerChestConfig load(Path file, UUID uuid) {

        file = rootDir.resolve(uuid.toString() + ".json");

        if (Files.exists(file)) {
            try {
                String json = Files.readString(file);
                PlayerChestConfig cfg = GSON.fromJson(json, PlayerChestConfig.class);
                if (cfg != null) {
                    if (cfg.items == null) cfg.items = new ArrayList<>();
                    cfg.uuid = uuid;
                    return cfg;
                }
            } catch (IOException ignored) { }
        }

        PlayerChestConfig cfg = new PlayerChestConfig();
        cfg.uuid = uuid;
        return cfg;
    }

    public void save(Path file) throws IOException {
        file = rootDir.resolve(uuid.toString() + ".json");

        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(this));
    }
}
