package io.github.halfmasa.xaerobinding.feature.placementpreview;
//#if MC >= 1.21.1
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
public enum PlacementPreviewFilter implements IConfigOptionListEntry {
    ALL, DIRECTIONAL, WHITELIST, BLACKLIST;
    @Override public String getStringValue(){return name().toLowerCase(java.util.Locale.ROOT);}
    @Override public String getDisplayName(){return StringUtils.translate("halfmasa.option.placement_preview."+getStringValue());}
    @Override public PlacementPreviewFilter cycle(boolean forward){return values()[Math.floorMod(ordinal()+(forward?1:-1),values().length)];}
    @Override public PlacementPreviewFilter fromString(String value){for(var m:values())if(m.getStringValue().equalsIgnoreCase(value))return m;return ALL;}
}
//#endif
