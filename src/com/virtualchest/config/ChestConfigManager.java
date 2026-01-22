package com.virtualchest.config;

import com.hypixel.hytale.server.core.universe.system.PlayerRefAddedSystem;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChestConfigManager {

    private static Path rootDir = Path.of("mods/VirtualChest");

    private static final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, PlayerChestConfig> cache = new ConcurrentHashMap<>();

    public ChestConfigManager(Path rootDir) {
        ChestConfigManager.rootDir = rootDir;
    }

    private static Path file(UUID uuid) {
        return rootDir.resolve(uuid.toString() + ".json");
    }

    /**
     * Загружает конфиг с диска (или создаёт новый) и кладёт в cache.
     * Повторный вызов НЕ перечитывает файл.
     */
    public static PlayerChestConfig load(UUID uuid) {
        return cache.computeIfAbsent(uuid, id ->
                PlayerChestConfig.load(file(id), id)
        );
    }

    /**
     * Возвращает конфиг ТОЛЬКО из cache.
     * Если не загружен — вернёт null.
     */
    public static PlayerChestConfig get(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * Сохраняет конфиг, если он загружен.
     */
    public static void save(UUID uuid) {

        // CASE 1: сохранить ВСЕ (uuid == null)
        if (uuid == null) {
            for (Map.Entry<UUID, PlayerChestConfig> entry : cache.entrySet()) {
                UUID id = entry.getKey();
                PlayerChestConfig cfg = entry.getValue();
                if (cfg == null) continue;

                try {
                    cfg.save(file(id));
                } catch (Exception e) {
                    System.err.println("Failed to save chest config for " + id);
                    e.printStackTrace();
                }
            }
            return;
        }

        // CASE 2: сохранить одного
        PlayerChestConfig cfg = cache.get(uuid);
        if (cfg == null) return;

        try {
            cfg.save(file(uuid));
        } catch (Exception e) {
            System.err.println("Failed to save chest config for " + uuid);
            e.printStackTrace();
        }
    }


    /**
     * Сохраняет и удаляет из cache.
     */
    public static void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
        dirty.remove(uuid);
    }

    /**
     * Сохраняет все загруженные конфиги (shutdown/autosave).
     */
    public static void saveAll() {
        for (UUID uuid : cache.keySet()) {
            save(uuid);
        }
    }

    public static void markDirty(UUID uuid) {
        if (uuid == null) return;
        dirty.add(uuid);
    }

    public static void flushDirty() {
        // снимаем снапшот и очищаем, чтобы не держать lock и не сохранять бесконечно
        UUID[] ids = dirty.toArray(new UUID[0]);
        java.util.Arrays.asList(ids).forEach(dirty::remove);

        for (UUID id : ids) {
            save(id);
            dirty.remove(id);
        }
    }
}
