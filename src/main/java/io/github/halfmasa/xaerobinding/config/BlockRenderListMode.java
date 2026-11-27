package io.github.halfmasa.xaerobinding.config;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum BlockRenderListMode implements IConfigOptionListEntry {
    BLACKLIST("blacklist"),
    WHITELIST("whitelist"),
    NONE("none");

    private final String value;

    BlockRenderListMode(String value) { this.value = value; }

    @Override public String getStringValue() { return value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.block_render_mode." + value); }
    @Override public BlockRenderListMode cycle(boolean forward) {
        return values()[Math.floorMod(ordinal() + (forward ? 1 : -1), values().length)];
    }
    @Override public BlockRenderListMode fromString(String value) {
        for (var mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) return mode;
        }
        return BLACKLIST;
    }
}
//#endif
