package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum ServerIconMatchMode implements IConfigOptionListEntry
{
    NAME_AND_IP("name_and_ip"),
    NAME_ONLY("name_only"),
    IP_ONLY("ip_only");

    private final String value;

    ServerIconMatchMode(String value)
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
        return StringUtils.translate("halfmasa.option.server_icon_match." + this.value);
    }

    @Override
    public ServerIconMatchMode cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(this.ordinal() + offset) % values().length];
    }

    @Override
    public ServerIconMatchMode fromString(String value)
    {
        for (ServerIconMatchMode mode : values())
        {
            if (mode.value.equalsIgnoreCase(value))
            {
                return mode;
            }
        }
        return NAME_AND_IP;
    }
}
