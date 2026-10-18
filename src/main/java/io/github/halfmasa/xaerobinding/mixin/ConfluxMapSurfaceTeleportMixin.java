package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import java.util.UUID;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.compat.ConfluxMapCapabilities;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.ConfluxMapLegacyTeleport;

/** Selected only for the dual-lookup legacy ABI, mutually exclusive with the public resolver. */
@Pseudo
@Mixin(targets = "cn.net.rms.confluxmap.mc.ui.screen.FullscreenMapScreen", remap = false)
public abstract class ConfluxMapSurfaceTeleportMixin
{
    @Unique private ConfluxMapLegacyTeleport halfmasa$legacyTeleport;
    @Unique private boolean halfmasa$legacyFailed;

    @Unique private ConfluxMapLegacyTeleport halfmasa$adapter() throws ReflectiveOperationException
    {
        if (halfmasa$legacyTeleport == null) halfmasa$legacyTeleport = new ConfluxMapLegacyTeleport(this.getClass());
        return halfmasa$legacyTeleport;
    }

    @WrapOperation(method = "locationMenuButtonSpecs", at = @At(value = "INVOKE",
            target = "Lcn/net/rms/confluxmap/mc/ui/screen/FullscreenMapLocationMenu;teleportPositionKnown(Lcn/net/rms/confluxmap/mc/ui/screen/FullscreenMapLocationMenu$Target;Z)Z"), require = 1, remap = false)
    private boolean halfmasa$allowFallback(@Coerce Object target, boolean live, Operation<Boolean> original)
    {
        boolean nativeKnown = original.call(target, live);
        if (nativeKnown || halfmasa$legacyFailed) return nativeKnown;
        try
        {
            return halfmasa$adapter().needsFallback(this, target);
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException exception)
        {
            halfmasa$fail(exception);
            return nativeKnown;
        }
    }

    @Inject(method = "runLocationAction", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void halfmasa$resolveFallback(@Coerce Object action, @Coerce Object target, @Coerce Object waypoint,
            UUID playerId, @Coerce Object returnScreen, CallbackInfo ci)
    {
        if (halfmasa$legacyFailed || !"TELEPORT".equals(String.valueOf(action))) return;
        try
        {
            if (halfmasa$adapter().teleport(this, target))
            {
                halfmasa$close(action);
                ci.cancel();
            }
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException exception) { halfmasa$fail(exception); }
    }

    @Inject(method = "runLocationAction", at = @At("TAIL"), require = 1, remap = false)
    private void halfmasa$closeAfterTeleport(@Coerce Object action, @Coerce Object target, @Coerce Object waypoint,
            UUID playerId, @Coerce Object returnScreen, CallbackInfo ci)
    { halfmasa$close(action); }

    @Unique private void halfmasa$close(Object action)
    {
        if (!ConfluxMapCapabilities.event("locationActionPerformed") && Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue()
                && Configs.CONFLUX_MAP_CLOSE_AFTER_TELEPORT.getBooleanValue() && "TELEPORT".equals(String.valueOf(action)))
        {
            Minecraft client = Minecraft.getInstance();
            if (MinecraftClientCompat.getScreen(client) == (Object)this) MinecraftClientCompat.setScreen(client, null);
        }
    }

    @Unique private void halfmasa$fail(Throwable exception)
    {
        if (!halfmasa$legacyFailed)
            XaeroWorldBinding.LOGGER.warn("Conflux Map legacy teleport contract changed; fallback height disabled", exception);
        halfmasa$legacyFailed = true;
    }
}
//#endif
