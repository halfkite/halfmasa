package io.github.halfmasa.xaerobinding.feature.minihud;

//#if MC >= 26.3
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.interfaces.IInventoryOverlayHandler;
import fi.dy.masa.malilib.render.GuiContext;
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

    public static void render(GuiContext context, ProfilerFiller profiler)
    {
        if (!INSTALLED || !Configs.MINIHUD_PREVIEW_INDEPENDENT.getBooleanValue() || context == null || profiler == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        Access current = resolve();
        if (current == null || current.main.getBooleanValue() || !current.enabled.getBooleanValue()
                || !current.key.isKeybindHeld()) return;
        // Do not change the main toggle, replay the full HUD callback, or add a second native preview.
        current.handler.getRenderContext(context, profiler);
    }

    private static Access resolve()
    {
        if (resolved) return access;
        resolved = true;
        try
        {
            Class<?> generic = Class.forName("fi.dy.masa.minihud.config.Configs$Generic");
            Class<?> hotkeys = Class.forName("fi.dy.masa.minihud.config.Hotkeys");
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
