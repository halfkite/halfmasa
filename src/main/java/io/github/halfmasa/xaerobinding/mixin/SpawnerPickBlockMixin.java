package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.SpawnerPickCapture;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 1.21.4
@Mixin(MultiPlayerGameMode.class)
//#else
//$$ @Mixin(net.minecraft.client.Minecraft.class)
//#endif
public abstract class SpawnerPickBlockMixin
{
    //#if MC >= 1.21.4
    @Inject(method = "handlePickItemFromBlock", at = @At("HEAD"))
    private void halfmasa_captureSpawnerState(BlockPos pos, boolean includeData, CallbackInfo ci)
    {
        SpawnerPickCapture.getInstance().capture(pos, includeData);
    }
    //#else
    //$$ @Inject(method = "pickBlock", at = @At("HEAD"))
    //$$ private void halfmasa_captureLocalSpawner(CallbackInfo ci) {
    //$$     var client = net.minecraft.client.Minecraft.getInstance();
    //$$     if (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) SpawnerPickCapture.getInstance().capture(hit.getBlockPos(), net.minecraft.client.gui.screens.Screen.hasControlDown());
    //$$ }
    //$$ @Inject(method = "pickBlock", at = @At("TAIL"))
    //$$ private void halfmasa_completeLocalSpawner(CallbackInfo ci) {
    //$$     var client = net.minecraft.client.Minecraft.getInstance();
    //$$     if (client.player == null) return;
    //$$     SpawnerPickCapture.getInstance().acknowledge(client.player.getInventory().selected);
    //$$     SpawnerPickCapture.getInstance().onClientTick(client);
    //$$ }
    //#endif
}
//#endif
