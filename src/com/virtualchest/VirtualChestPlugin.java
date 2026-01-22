package com.virtualchest;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.virtualchest.commands.VirtualChestCommand;
import com.virtualchest.config.ChestConfigManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VirtualChestPlugin extends JavaPlugin {

    private ScheduledExecutorService autosave;

    public VirtualChestPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new VirtualChestCommand());

        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, VirtualChestPlugin::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, VirtualChestPlugin::onPlayerLeave);

        new ChestConfigManager(java.nio.file.Path.of("VirtualChest"));

        autosave = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VirtualChest-Autosave");
            t.setDaemon(true);
            return t;
        });

        autosave.scheduleAtFixedRate(() -> {
            try {
                ChestConfigManager.flushDirty();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 30, 30, TimeUnit.SECONDS); // каждые 30 секунд
    }

    private static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();

        ChestConfigManager.load(player.getUuid());
        
    }

    private static void onPlayerLeave(PlayerDisconnectEvent event) {
        PlayerRef player = event.getPlayerRef();

        UUID playerUuid = player.getUuid();
        if(playerUuid != null){
            ChestConfigManager.save(playerUuid);
            ChestConfigManager.unload(playerUuid);
        }
    }
}