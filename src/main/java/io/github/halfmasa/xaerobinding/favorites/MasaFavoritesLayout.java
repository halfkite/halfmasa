//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.Minecraft;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.mixin.GuiListBaseAccessor;

/** Rebuild at the next UI tick, after the button's input dispatch has finished. */
public final class MasaFavoritesLayout implements IClientTickHandler
{
    public static final MasaFavoritesLayout INSTANCE = new MasaFavoritesLayout();
    private boolean pending;
    private MasaFavoritesLayout() { }
    public void requestRefresh() { this.pending = true; }

    @Override
    public void onClientTick(Minecraft client)
    {
        if (!this.pending) return;
        this.pending = false;
        if (!(MinecraftClientCompat.getScreen(client) instanceof GuiConfigsBase screen)) return;
        if (!(((GuiListBaseAccessor) (Object) screen).halfmasa$getListWidget() instanceof WidgetListConfigOptions list)) return;
        int scroll = list.getScrollbar().getValue();
        list.applyPendingModifications();
        list.wereConfigsModified();
        screen.clearOptions();
        list.refreshEntries();
        list.getScrollbar().setValue(scroll);
        if (screen instanceof MasaSwitcherAccess access) access.halfmasa$rebuildSwitcher();
    }
}
//#endif
