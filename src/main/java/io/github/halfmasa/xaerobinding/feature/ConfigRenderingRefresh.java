package io.github.halfmasa.xaerobinding.feature;

import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.minecraft.client.Minecraft;

/** Keep rendering side effects independent of MaLiLib's JSON callback differences. */
public final class ConfigRenderingRefresh
{
    private static boolean initialized;
    private static boolean fluidsDisabled;
    private static boolean flowingFluidsDisabled;
    private static boolean translationSpacingEnabled;

    private ConfigRenderingRefresh() {}

    public static void onConfigLoaded()
    {
        if (!initialized)
        {
            // The first config load precedes world meshes and the normal language load.
            // Older MaLiLib versions assign JSON values without notifying callbacks.
            fluidsDisabled = Configs.DISABLE_FLUID_RENDERING.getBooleanValue();
            flowingFluidsDisabled = !fluidsDisabled && Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.getBooleanValue();
            translationSpacingEnabled = translationSpacingEnabled();
            initialized = true;
            return;
        }
        refreshFluids();
        refreshTranslations();
    }

    public static void refreshFluids()
    {
        // Older MaLiLib versions do not notify all callbacks when reading JSON. The
        // config loader invokes this once after loading; newer callbacks must wait too.
        if (!Configs.isConfigLoaded()) return;
        boolean fluids = Configs.DISABLE_FLUID_RENDERING.getBooleanValue();
        boolean flowing = !fluids && Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.getBooleanValue();
        if (fluids == fluidsDisabled && flowing == flowingFluidsDisabled) return;
        fluidsDisabled = fluids;
        flowingFluidsDisabled = flowing;

        Minecraft client = Minecraft.getInstance();
        if (client != null && client.level != null)
            MinecraftClientCompat.reloadLevelRenderer(client);
    }

    public static void refreshTranslations()
    {
        if (!Configs.isConfigLoaded()) return;
        boolean enabled = translationSpacingEnabled();
        if (enabled == translationSpacingEnabled) return;
        translationSpacingEnabled = enabled;
        Minecraft client = Minecraft.getInstance();
        if (client != null)
            client.getLanguageManager().onResourceManagerReload(client.getResourceManager());
    }

    private static boolean translationSpacingEnabled()
    {
        return Configs.CJK_LATIN_SPACING.getBooleanValue()
                && Configs.CJK_LATIN_SPACING_TRANSLATIONS.getBooleanValue();
    }
}
