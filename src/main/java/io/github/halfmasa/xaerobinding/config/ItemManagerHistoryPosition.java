package io.github.halfmasa.xaerobinding.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum ItemManagerHistoryPosition implements IConfigOptionListEntry
{
    BOTTOM_RIGHT("bottom_right"),
    TOP_RIGHT("top_right"),
    TOP_LEFT("top_left"),
    BOTTOM_LEFT("bottom_left");

    private final String value;

    ItemManagerHistoryPosition(String value)
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
        return StringUtils.translate("halfmasa.option.item_manager_history_position." + this.value);
    }

    @Override
    public ItemManagerHistoryPosition cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(this.ordinal() + offset) % values().length];
    }

    @Override
    public ItemManagerHistoryPosition fromString(String value)
    {
        for (ItemManagerHistoryPosition position : values())
        {
            if (position.value.equalsIgnoreCase(value))
            {
                return position;
            }
        }
        return BOTTOM_RIGHT;
    }
}
