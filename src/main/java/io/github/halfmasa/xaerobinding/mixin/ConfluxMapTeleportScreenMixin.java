package io.github.halfmasa.xaerobinding.mixin;

import java.util.OptionalInt;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;

import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;

/** Closes Conflux Map's fullscreen map after its waypoint teleport action completes. */
@Pseudo
@Mixin(targets = "cn.net.rms.confluxmap.mc.ui.screen.FullscreenMapScreen", remap = false)
public abstract class ConfluxMapTeleportScreenMixin
{
    @Redirect(
            method = "locationMenuButtonSpecs",
            at = @At(value = "INVOKE", target = "Ljava/util/OptionalInt;isPresent()Z"),
            require = 0,
            remap = false)
    private boolean halfmasa$enableUnknownHeightTeleport(OptionalInt surfaceY)
    {
        return surfaceY.isPresent() || this.halfmasa$hasUnknownHeightFallback();
    }

    @ModifyArg(
            method = "runLocationAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lcn/net/rms/confluxmap/mc/teleport/ClientGroundTeleportService;teleport(IILjava/util/OptionalInt;Lcn/net/rms/confluxmap/core/model/DimensionId;Lcn/net/rms/confluxmap/core/model/WorldIdentity;Z)V"),
            index = 2,
            require = 0,
            remap = false)
    private OptionalInt halfmasa$useConfiguredUnknownHeight(OptionalInt surfaceY)
    {
        if (surfaceY.isPresent() || !this.halfmasa$hasUnknownHeightFallback())
        {
            return surfaceY;
        }
        return OptionalInt.of(Configs.CONFLUX_MAP_UNKNOWN_HEIGHT.getIntegerValue());
    }

    @Inject(method = "runLocationAction", at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa$closeAfterTeleport(
            @Coerce Object action,
            @Coerce Object target,
            @Coerce Object waypoint,
            UUID waypointId,
            @Coerce Object returnScreen,
            CallbackInfo ci)
    {
        if (!Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue() ||
                !Configs.CONFLUX_MAP_CLOSE_AFTER_TELEPORT.getBooleanValue() ||
                action == null || !action.toString().equals("TELEPORT"))
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (MinecraftClientCompat.getScreen(client) == (Object) this)
        {
            MinecraftClientCompat.setScreen(client, null);
        }
    }

    private boolean halfmasa$hasUnknownHeightFallback()
    {
        return Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue();
    }
}
