package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Passive bindings have no action callback and must never be conflict candidates. */
public final class HoldToShow
{
    private HoldToShow() {}

    public static boolean isPassive(IHotkey hotkey)
    {
        return hotkey == Configs.BOSS_BAR_HOLD_KEY || hotkey == Configs.INVISIBLE_HOLD_KEY
                || hotkey == Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY;
    }

    private static boolean held(ConfigHotkey hotkey)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.player.isAlive() ||
                MinecraftClientCompat.getScreen(mc) != null || !mc.isWindowActive() || hotkey.getKeybind().getKeys().isEmpty()) return false;
        // Refresh from MaLiLib's pressed inputs even when conflict selection skipped dispatch.
        hotkey.getKeybind().updateIsPressed();
        return hotkey.getKeybind().isKeybindHeld();
    }

    public static boolean entityRenderingRestored()
    {
        return Configs.ENTITY_RENDER_HOLD_SHOW.getBooleanValue() && held(Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY);
    }

    public static boolean bossVisible()
    {
        return !Configs.BETTER_BOSS_BAR.getBooleanValue() || !Configs.BOSS_BAR_HOLD_ONLY.getBooleanValue()
                || held(Configs.BOSS_BAR_HOLD_KEY);
    }

    public static boolean invisibleVisible()
    {
        return !Configs.INVISIBLE_HOLD_ONLY.getBooleanValue() || held(Configs.INVISIBLE_HOLD_KEY);
    }

    public static boolean selectedMarker(Block block)
    {
        return block == Blocks.BARRIER && Configs.INVISIBLE_SHOW_BARRIER.getBooleanValue()
                || block == Blocks.LIGHT && Configs.INVISIBLE_SHOW_LIGHT.getBooleanValue()
                || block == Blocks.STRUCTURE_VOID && Configs.INVISIBLE_SHOW_STRUCTURE_VOID.getBooleanValue();
    }

    public static boolean hideMarker(Block block)
    {
        return Configs.INVISIBLE_BLOCKS.getBooleanValue() && selectedMarker(block) && !invisibleVisible();
    }
}
//#endif
