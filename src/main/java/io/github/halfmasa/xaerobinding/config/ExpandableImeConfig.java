package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;

public final class ExpandableImeConfig extends ConfigBooleanHotkeyed
{
    public ExpandableImeConfig(String name, boolean defaultValue, String defaultHotkey, String translationKey)
    {
        super(name, defaultValue, defaultHotkey);
        this.apply(translationKey);
    }
}
