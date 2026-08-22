package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;

public final class ConfigGroupHeader extends ConfigHotkey
{
    private final ConfigBoolean expansion;

    public ConfigGroupHeader(String name, String translationKey, ConfigBoolean expansion)
    {
        super(name, "");
        super.apply(translationKey);
        this.expansion = expansion;
    }

    public ConfigBoolean getExpansion()
    {
        return this.expansion;
    }
}
