package io.github.halfmasa.xaerobinding.config;

import java.util.function.BooleanSupplier;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

public final class ActionHotkey extends ConfigHotkey
{
    private BooleanSupplier action = () -> false;

    public ActionHotkey(String name, String defaultHotkey)
    {
        super(name, defaultHotkey);
    }

    public ActionHotkey(String name, String defaultHotkey, KeybindSettings settings)
    {
        super(name, defaultHotkey, settings);
    }

    public ActionHotkey applyTranslationKey(String translationKey)
    {
        super.apply(translationKey);
        return this;
    }

    public void setAction(BooleanSupplier action)
    {
        this.action = action;
    }

    public boolean trigger()
    {
        return this.action.getAsBoolean();
    }
}
