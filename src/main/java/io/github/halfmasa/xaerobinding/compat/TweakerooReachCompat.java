package io.github.halfmasa.xaerobinding.compat;

//#if MC >= 1.21.1
import net.fabricmc.loader.api.FabricLoader;
import fi.dy.masa.tweakeroo.config.FeatureToggle;

/** Optional dependency: the bridge is only reached when Tweakeroo is loaded. */
public final class TweakerooReachCompat
{
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("tweakeroo");

    private TweakerooReachCompat() {}

    public static boolean isBlockOverrideEnabled()
    {
        return LOADED && Bridge.blockOverride();
    }

    public static boolean isEntityOverrideEnabled()
    {
        return LOADED && Bridge.entityOverride();
    }

    private static final class Bridge
    {
        static boolean blockOverride()
        {
            return FeatureToggle.TWEAK_BLOCK_REACH_OVERRIDE.getBooleanValue();
        }

        static boolean entityOverride()
        {
            return FeatureToggle.TWEAK_ENTITY_REACH_OVERRIDE.getBooleanValue();
        }
    }
}
//#endif
