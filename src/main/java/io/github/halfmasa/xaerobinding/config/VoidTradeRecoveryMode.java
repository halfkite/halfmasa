package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum VoidTradeRecoveryMode implements IConfigOptionListEntry
{
    REJOIN("rejoin"),
    SPAWN_MOUNT("spawn_mount");

    private final String value;

    VoidTradeRecoveryMode(String value)
    {
        this.value = value;
    }

    @Override
    public String getStringValue()
    {
        return this.value;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate("halfmasa.option.void_trading.recovery." + this.value);
    }

    @Override
    public VoidTradeRecoveryMode cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(this.ordinal() + offset) % values().length];
    }

    @Override
    public VoidTradeRecoveryMode fromString(String value)
    {
        for (VoidTradeRecoveryMode mode : values())
        {
            if (mode.value.equalsIgnoreCase(value)) return mode;
        }
        return REJOIN;
    }
}
