package io.github.halfmasa.xaerobinding.config;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum ConfluxMapWaypointOrder implements IConfigOptionListEntry
{
    SERVER_FIRST("server_first"), PERSONAL_FIRST("personal_first");
    private final String value;
    ConfluxMapWaypointOrder(String value) { this.value = value; }
    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.conflux_map_waypoint_order." + this.value); }
    @Override public ConfluxMapWaypointOrder cycle(boolean forward) { return this == SERVER_FIRST ? PERSONAL_FIRST : SERVER_FIRST; }
    @Override public ConfluxMapWaypointOrder fromString(String value)
    {
        for (var entry : values()) if (entry.value.equalsIgnoreCase(value)) return entry;
        return SERVER_FIRST;
    }
}
//#endif
