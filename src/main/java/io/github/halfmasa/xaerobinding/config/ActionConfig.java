package io.github.halfmasa.xaerobinding.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.options.ConfigBase;

public final class ActionConfig extends ConfigBase<ActionConfig>
{
    private final List<ActionButton> buttons;

    public ActionConfig(String name, String... buttonTranslationKeys)
    {
        super(ConfigType.STRING, name);
        this.buttons = new ArrayList<>(buttonTranslationKeys.length);
        for (String translationKey : buttonTranslationKeys)
        {
            this.buttons.add(new ActionButton(translationKey, () -> false));
        }
    }

    public ActionConfig applyTranslationKey(String translationKey)
    {
        super.apply(translationKey);
        return this;
    }

    public List<ActionButton> getButtons()
    {
        return List.copyOf(this.buttons);
    }

    public void setAction(int index, BooleanSupplier action)
    {
        ActionButton current = this.buttons.get(index);
        this.buttons.set(index, new ActionButton(current.translationKey(), action));
    }

    @Override
    public void setValueFromJsonElement(JsonElement element)
    {
    }

    @Override
    public JsonElement getAsJsonElement()
    {
        return JsonNull.INSTANCE;
    }

    @Override
    public boolean isModified()
    {
        return false;
    }

    @Override
    public void resetToDefault()
    {
    }

    public record ActionButton(String translationKey, BooleanSupplier action)
    {
        public boolean trigger()
        {
            return this.action.getAsBoolean();
        }
    }
}
