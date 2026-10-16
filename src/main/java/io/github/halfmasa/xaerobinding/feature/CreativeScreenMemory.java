package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 26.3
import java.util.IdentityHashMap;
import java.util.Map;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

/** Browsing state belongs to the running client, never to the configuration file. */
public final class CreativeScreenMemory
{
    public interface ContentsReady
    {
        void halfmasa$restoreCreativePosition(boolean tabSelectionFinished);
    }

    private static final Map<CreativeModeTab, Float> SCROLLS = new IdentityHashMap<>();
    private static CreativeModeTab lastTab;
    private static String searchText = "";
    private static int lastPage;

    private CreativeScreenMemory() {}

    public static boolean enabled()
    {
        return Configs.KEEP_SCREEN_POSITION.getBooleanValue()
                && Configs.KEEP_CREATIVE_SCREEN_POSITION.getBooleanValue();
    }

    public static void saveTab(CreativeModeTab tab, float scroll, String query)
    {
        if (!enabled() || tab == null) return;
        SCROLLS.put(tab, Float.isFinite(scroll) ? Math.clamp(scroll, 0F, 1F) : 0F);
        if (tab == CreativeModeTabs.searchTab()) searchText = query == null ? "" : query;
    }

    public static void saveClose(CreativeModeTab tab, float scroll, String query, int page)
    {
        if (!enabled() || tab == null) return;
        saveTab(tab, scroll, query);
        lastTab = tab;
        lastPage = Math.max(0, page);
    }

    public static CreativeModeTab restoreTab()
    {
        if (!enabled() || lastTab == null) return null;
        if (TrialCreativeTab.isTrialTab(lastTab) && !TrialCreativeTab.isRegistered())
            return TrialCreativeTab.isEnabled() ? lastTab : null;
        return CreativeModeTabs.allTabs().contains(lastTab) && lastTab.shouldDisplay() ? lastTab : null;
    }

    public static float restoreScroll(CreativeModeTab tab)
    {
        return enabled() ? SCROLLS.getOrDefault(tab, 0F) : 0F;
    }

    public static String restoreSearch() { return enabled() ? searchText : ""; }
    public static int restorePage() { return enabled() ? lastPage : 0; }

    private static boolean hasPages()
    {
        return FabricLoader.getInstance().isModLoaded("fabric-creative-tab-api-v1");
    }

    public static int currentPage(CreativeModeInventoryScreen screen)
    {
        return hasPages() ? CreativeScreenMemoryPages.currentPage(screen) : 0;
    }

    public static void restorePage(CreativeModeInventoryScreen screen, CreativeModeTab tab)
    {
        if (enabled() && hasPages()) CreativeScreenMemoryPages.restorePage(screen, tab, lastPage);
    }

    /** Called after both condensed entries and recent search items have been rebuilt. */
    public static void contentsReady(CreativeModeInventoryScreen screen, boolean tabSelectionFinished)
    {
        ((ContentsReady) screen).halfmasa$restoreCreativePosition(tabSelectionFinished);
    }

    public static void clear()
    {
        SCROLLS.clear();
        lastTab = null;
        searchText = "";
        lastPage = 0;
    }
}
//#endif
