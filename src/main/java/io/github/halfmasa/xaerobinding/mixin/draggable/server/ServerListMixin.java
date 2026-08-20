package io.github.halfmasa.xaerobinding.mixin.draggable.server;

import io.github.halfmasa.xaerobinding.draggable.DragItem;
import io.github.halfmasa.xaerobinding.draggable.duck.ServerListDuckProvider;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ServerList.class)
public class ServerListMixin implements ServerListDuckProvider {
    @Shadow
    @Final
    private List<ServerData> serverList;

    @Override
    public void draggable_lists$moveItem(DragItem<ServerData, ServerSelectionList.OnlineServerEntry> item, int position) {
        ServerData serverData = item.draggable_lists$getUnderlyingData();
        serverList.remove(serverData);
        serverList.add(position, serverData);
    }
}
