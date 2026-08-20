package io.github.halfmasa.xaerobinding.draggable;

import io.github.halfmasa.xaerobinding.config.Configs;

public final class DraggableLists
{
    public static final Settings CONFIG = new Settings();
    private DraggableLists() {}

    public static final class Settings
    {
        public boolean disableResourcePackArrows() { return Configs.DRAG_HIDE_RESOURCE_ARROWS.getBooleanValue(); }
        public boolean disableServerArrows() { return Configs.DRAG_HIDE_SERVER_ARROWS.getBooleanValue(); }
    }
}
