package io.github.halfmasa.xaerobinding.feature;

import io.github.halfmasa.xaerobinding.config.Configs;

public final class FastScrolling
{
    private FastScrolling() {}

    public static boolean isActive()
    {
        return Configs.FAST_SCROLLING.getBooleanValue() &&
                (isSecondaryActive() || isPrimaryActive());
    }

    public static int getMultiplier()
    {
        if (!Configs.FAST_SCROLLING.getBooleanValue())
        {
            return 1;
        }
        if (isSecondaryActive())
        {
            return Configs.FAST_SCROLLING_SECONDARY_MULTIPLIER.getIntegerValue();
        }
        if (isPrimaryActive())
        {
            return Configs.FAST_SCROLLING_PRIMARY_MULTIPLIER.getIntegerValue();
        }
        return 1;
    }

    private static boolean isPrimaryActive()
    {
        return Configs.FAST_SCROLLING_PRIMARY_ENABLED.getBooleanValue() &&
                Configs.FAST_SCROLLING_PRIMARY_HOTKEY.getKeybind().isKeybindHeld();
    }

    private static boolean isSecondaryActive()
    {
        return Configs.FAST_SCROLLING_SECONDARY_ENABLED.getBooleanValue() &&
                Configs.FAST_SCROLLING_SECONDARY_HOTKEY.getKeybind().isKeybindHeld();
    }
}
