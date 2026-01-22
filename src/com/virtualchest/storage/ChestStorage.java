package com.virtualchest.storage;

import java.util.Set;
import java.util.UUID;

public interface ChestStorage {

    public Set<ChestItem> loadItems(UUID playerUuid);
    public void saveItems(UUID playerUuid, Set<ChestItem> itemSet);

    public class ChestItem {
        public String itemId;
        public int quantity;
        public short slot;
    }
}