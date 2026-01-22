package com.virtualchest.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.virtualchest.containers.VirtualChestContainer;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class VirtualChestCommand extends AbstractPlayerCommand {

    public VirtualChestCommand() {
        super("virtualchest", "Открывает виртуальный сундук");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        Player player = store.getComponent(ref, Player.getComponentType());

        assert player != null;
        if(player.getGameMode() == GameMode.Creative){
            player.sendMessage(Message.raw("Виртуальный сундук нельзя открыть в креативе!"));
        }

        VirtualChestContainer virtualChestContainer = new VirtualChestContainer(player.getUuid());

        ContainerWindow chestWindow = new ContainerWindow(virtualChestContainer);
        PageManager pageManager = player.getPageManager();

        pageManager.setPageWithWindows(ref, store, Page.Bench, true, chestWindow);

    }
}
