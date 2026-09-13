package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;

public final class ConfigGroupHeader extends ConfigHotkey
{
    private final ConfigBoolean expansion;
    private final String displayName;

    public ConfigGroupHeader(String name, String translationKey, ConfigBoolean expansion)
    {
        this(name, translationKey, expansion, null);
    }

    public ConfigGroupHeader(String name, String translationKey, ConfigBoolean expansion, String displayName)
    {
        super(name, "");
        super.apply(translationKey);
        this.expansion = expansion;
        this.displayName = displayName;
    }

    public ConfigBoolean getExpansion()
    {
        return this.expansion;
    }

    @Override
    public String getConfigGuiDisplayName()
    {
        return this.displayName != null && !this.displayName.isBlank()
                ? this.displayName : super.getConfigGuiDisplayName();
    }
}
