package io.github.halfmasa.xaerobinding.mixin;

import java.lang.reflect.Field;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;

/**
 * Applies the configured initial filters to Conflux Map's waypoint-list hotkey.
 * Its published API does not currently expose these screen defaults, so this
 * optional adapter only touches the initial screen state and has no hard dependency.
 */
@Pseudo
@Mixin(targets = "cn.net.rms.confluxmap.mc.ui.screen.WaypointListScreen", remap = false)
public abstract class ConfluxMapWaypointListMixin
{
    @Unique
    private static boolean halfmasa$warnedConfluxMapContract;

    @Unique
    private boolean halfmasa$appliedWaypointListDefaults;

    @Inject(method = "init", at = @At("HEAD"), require = 0, remap = false)
    private void halfmasa$applyWaypointListDefaults(CallbackInfo ci)
    {
        if (this.halfmasa$appliedWaypointListDefaults)
        {
            return;
        }

        try
        {
            if (!this.halfmasa$isOpenedFromHotkey())
            {
                return;
            }

            this.halfmasa$appliedWaypointListDefaults = true;
            if (!Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue())
            {
                return;
            }

            if (Configs.CONFLUX_MAP_ALL_DIMENSIONS.getBooleanValue())
            {
                Class<?> filterClass = Class.forName(
                        "cn.net.rms.confluxmap.core.waypoint.WaypointDimensionFilter",
                        true,
                        this.getClass().getClassLoader());
                Object allDimensions = filterClass.getMethod("all").invoke(null);
                this.halfmasa$getField("dimensionFilter").set(this, allDimensions);
            }

            if (Configs.CONFLUX_MAP_PUBLIC_WAYPOINTS.getBooleanValue() && this.halfmasa$hasPublicWaypointTab())
            {
                Class<?> tabClass = Class.forName(
                        "cn.net.rms.confluxmap.mc.ui.screen.WaypointListScreen$Tab",
                        true,
                        this.getClass().getClassLoader());
                Object publicTab = tabClass.getField("PUBLIC").get(null);
                this.halfmasa$getField("tab").set(this, publicTab);
            }
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException exception)
        {
            if (!halfmasa$warnedConfluxMapContract)
            {
                halfmasa$warnedConfluxMapContract = true;
                XaeroWorldBinding.LOGGER.warn(
                        "Conflux Map changed its waypoint-list internals; HalfMasa could not apply the configured defaults",
                        exception);
            }
        }
    }

    @Unique
    private boolean halfmasa$isOpenedFromHotkey() throws ReflectiveOperationException
    {
        return this.halfmasa$getField("openedFromHotkey").getBoolean(this);
    }

    @Unique
    private boolean halfmasa$hasPublicWaypointTab() throws ReflectiveOperationException
    {
        Object sharedWaypoints = this.halfmasa$getField("sharedWaypoints").get(this);
        Object availability = sharedWaypoints.getClass().getMethod("availability").invoke(sharedWaypoints);
        return Boolean.TRUE.equals(availability.getClass().getMethod("enabled").invoke(availability));
    }

    @Unique
    private Field halfmasa$getField(String name) throws ReflectiveOperationException
    {
        Field field = this.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
