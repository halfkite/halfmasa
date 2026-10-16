package io.github.halfmasa.xaerobinding.feature.minihud;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.interfaces.IInventoryOverlayHandler;
//#if MC > 1.21.10
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;

/** Optional linkage is resolved once; all per-frame calls use existing MaLiLib interfaces. */
public final class MiniHudPreviewBridge
{
    private static final boolean INSTALLED = FabricLoader.getInstance().isModLoaded("minihud");
    private static boolean resolved;
    private static Access access;

    private record Access(IConfigBoolean main, IConfigBoolean enabled, IKeybind key,
                          IInventoryOverlayHandler handler) {}

    private MiniHudPreviewBridge() {}

    //#if MC > 1.21.10
    public static void render(GuiContext context, ProfilerFiller profiler)
    //#else
    //$$ public static void render(GuiGraphics context, ProfilerFiller profiler)
    //#endif
    {
        if (!INSTALLED || !Configs.MINIHUD_PREVIEW_INDEPENDENT.getBooleanValue() || context == null || profiler == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        Access current = resolve();
        if (current == null || current.main.getBooleanValue() || !current.enabled.getBooleanValue()
                || !current.key.isKeybindHeld()) return;
        // Do not change the main toggle, replay the full HUD callback, or add a second native preview.
        //#if MC > 1.21.10
        current.handler.getRenderContext(context, profiler);
        //#else
        //$$ current.handler.getRenderContext(context, profiler, client);
        //#endif
    }

    private static Access resolve()
    {
        if (resolved) return access;
        resolved = true;
        try
        {
            Class<?> generic = Class.forName("fi.dy.masa.minihud.config.Configs$Generic");
            //#if MC >= 26.3
            Class<?> hotkeys = Class.forName("fi.dy.masa.minihud.config.Hotkeys");
            //#else
            //$$ Class<?> hotkeys = generic;
            //#endif
            Object main = generic.getField("MAIN_RENDERING_TOGGLE").get(null);
            Object enabled = generic.getField("INVENTORY_PREVIEW_ENABLED").get(null);
            Object previewKey = hotkeys.getField("INVENTORY_PREVIEW").get(null);
            Object handler = Class.forName("fi.dy.masa.minihud.renderer.InventoryOverlayHandler")
                    .getMethod("getInstance").invoke(null);
            if (!(main instanceof IConfigBoolean mainConfig) || !(enabled instanceof IConfigBoolean previewConfig)
                    || !(previewKey instanceof IHotkey hotkey) || !(handler instanceof IInventoryOverlayHandler overlay))
                throw new ReflectiveOperationException("MiniHUD preview objects do not implement the expected MaLiLib interfaces");
            access = new Access(mainConfig, previewConfig, hotkey.getKeybind(), overlay);
        }
        catch (ReflectiveOperationException | LinkageError exception)
        {
            XaeroWorldBinding.LOGGER.warn("MiniHUD independent preview: incompatible optional API; keeping native behavior", exception);
        }
        return access;
    }
}
//#endif
