package com.virtualchest.containers;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.function.consumer.ShortObjectConsumer;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterActionType;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType;
import com.hypixel.hytale.server.core.inventory.container.filter.SlotFilter;
import com.hypixel.hytale.server.core.inventory.transaction.ClearTransaction;
import com.virtualchest.config.ChestConfigManager;
import com.virtualchest.config.PlayerChestConfig;
import com.virtualchest.storage.ChestStorage;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class VirtualChestContainer extends ItemContainer {

    // ВАЖНО: для player-specific контейнера singleton опасен.
    // Но оставляю, раз у вас CODEC требует INSTANCE.
    // Главное: clone() НЕ должен возвращать INSTANCE.
    public static final VirtualChestContainer INSTANCE = new VirtualChestContainer();
    public static final BuilderCodec<VirtualChestContainer> CODEC =
            BuilderCodec.builder(VirtualChestContainer.class, () -> INSTANCE).build();

    private short _capacity;

    private ItemStack[] _slots;

    private PlayerChestConfig _config;

    private UUID _playerUuid;
    private volatile boolean _initialized;

    public VirtualChestContainer(UUID playerUuid) {
        _playerUuid = playerUuid;
        _capacity = 27;

        _slots = null;
        _config = null;
        _initialized = false;
    }

    protected VirtualChestContainer() {
        // для CODEC/рефлексии
    }

    private void ensureLoaded() {
        if (_initialized) return;

        synchronized (this) {
            if (_initialized) return;

            if (_playerUuid == null) {
                throw new IllegalStateException("VirtualChestContainer: playerUuid is not set.");
            }

            _config = ChestConfigManager.load(_playerUuid);

            _slots = new ItemStack[_capacity];

            for (var item : _config.items) {
                int slot = item.slot;
                if (slot < 0 || slot >= _capacity) continue;

                _slots[slot] = new ItemStack(item.itemId, item.quantity);
            }

            _initialized = true;
        }
    }

    public short getCapacity() {
        return _capacity;
    }

    @Nonnull
    public ClearTransaction clear() {
        return ClearTransaction.EMPTY;
    }

    @Override
    public void forEach(@NonNullDecl ShortObjectConsumer<ItemStack> action) {
        ensureLoaded();
        for (short i = 0; i < _capacity; i++) {
            action.accept(i, _slots[i]);
        }
    }

    protected <V> V readAction(@Nonnull Supplier<V> action) {
        return (V) action.get();
    }

    protected <X, V> V readAction(@Nonnull Function<X, V> action, X x) {
        return (V) action.apply(x);
    }

    protected <V> V writeAction(@Nonnull Supplier<V> action) {
        return (V) action.get();
    }

    protected <X, V> V writeAction(@Nonnull Function<X, V> action, X x) {
        return (V) action.apply(x);
    }

    @Nonnull
    protected ClearTransaction internal_clear() {
        return ClearTransaction.EMPTY;
    }

    protected ItemStack internal_getSlot(short slot) {
        ensureLoaded();
        validateSlotIndex(slot, _capacity);
        return _slots[slot];
    }

    @Override
    protected ItemStack internal_setSlot(short slot, ItemStack itemStack) {
        ensureLoaded();
        validateSlotIndex(slot, _capacity);

        ItemStack prev = _slots[slot];
        _slots[slot] = itemStack;

        upsertConfigSlot(slot, itemStack);

        ChestConfigManager.markDirty(_playerUuid);
        return prev;
    }

    private void upsertConfigSlot(short slot, ItemStack stack) {
        _config.items.removeIf(i -> i.slot == slot);

        if (stack == null) return;

        ChestStorage.ChestItem ci = new ChestStorage.ChestItem();
        ci.slot = slot;
        ci.itemId = stack.getItemId();
        ci.quantity = stack.getQuantity();
        _config.items.add(ci);
    }

    private void removeConfigSlot(short slot) {
        _config.items.removeIf(i -> i.slot == slot);
    }

    @Override
    protected ItemStack internal_removeSlot(short slot) {
        ensureLoaded();
        validateSlotIndex(slot, _capacity);

        ItemStack prev = _slots[slot];
        _slots[slot] = null;

        removeConfigSlot(slot);
        ChestConfigManager.markDirty(_playerUuid);
        return prev;
    }

    protected boolean cantAddToSlot(short slot, ItemStack itemStack, ItemStack slotItemStack) {
        return false;
    }

    protected boolean cantRemoveFromSlot(short slot) {
        return false;
    }

    protected boolean cantDropFromSlot(short slot) {
        return false;
    }

    protected boolean cantMoveToSlot(ItemContainer fromContainer, short slotFrom) {
        return false;
    }

    @Nonnull
    public List<ItemStack> removeAllItemStacks() {
        return Collections.emptyList();
    }

    @Nonnull
    public Map<Integer, ItemWithAllMetadata> toProtocolMap() {
        ensureLoaded();

        var map = new java.util.HashMap<Integer, ItemWithAllMetadata>();

        for (int i = 0; i < _capacity; i++) {
            ItemStack s = _slots[i];
            if (s == null) continue;

            ItemWithAllMetadata p = new ItemWithAllMetadata();
            p.itemId = s.getItemId();
            p.quantity = s.getQuantity();
            p.durability = 0;
            p.maxDurability = 0;
            p.overrideDroppedItemAnimation = false;
            p.metadata = null;

            map.put(i, p);
        }

        return map;
    }

    // FIX 1: clone() не должен возвращать INSTANCE
    public VirtualChestContainer clone() {
        VirtualChestContainer c = new VirtualChestContainer();
        c._capacity = this._capacity;
        c._playerUuid = this._playerUuid;
        c._initialized = this._initialized;

        c._config = this._config;

        if (this._slots != null) {
            c._slots = this._slots.clone();
        }

        return c;
    }

    // FIX 2 (главное): registerChangeEvent должен реально регистрировать consumer
    // В идеале — делегировать базовой реализации.
    public EventRegistration registerChangeEvent(short priority, @NonNullDecl Consumer<ItemContainerChangeEvent> consumer) {
        return super.registerChangeEvent(priority, consumer);
    }

    public void setGlobalFilter(FilterType globalFilter) { }

    public void setSlotFilter(FilterActionType actionType, short slot, SlotFilter filter) {
        validateSlotIndex(slot, _capacity);
    }
}
