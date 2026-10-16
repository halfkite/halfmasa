package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MiniHudPreviewLifecycleMixin
{
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void halfmasa$worldChanged(CallbackInfo ci) { PreviewCache.clear(); }

    //#if MC >= 26.2
    @Inject(method = "reloadResourcePacks(ZLnet/minecraft/client/GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "reloadResourcePacks(ZLnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    //#endif
    private void halfmasa$resourcesChanged(CallbackInfoReturnable<?> ci) { PreviewCache.clear(); }
}
//#endif
