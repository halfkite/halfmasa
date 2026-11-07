//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.halfmasa.xaerobinding.config.Configs;

public enum MasaFavoritePosition implements IConfigOptionListEntry
{
    LEFT("left"), RIGHT("right");
    private final String value;
    MasaFavoritePosition(String value) { this.value = value; }
    @Override public String getStringValue() { return this.value; }
    @Override public String getDisplayName() { return StringUtils.translate("halfmasa.option.favorite_position." + this.value); }
    @Override public MasaFavoritePosition cycle(boolean forward) { return this == LEFT ? RIGHT : LEFT; }
    @Override public MasaFavoritePosition fromString(String value) { return "left".equalsIgnoreCase(value) ? LEFT : RIGHT; }
    public static boolean isLeft() { return Configs.MASA_FAVORITES_POSITION.getOptionListValue() == LEFT; }
}
//#endif
