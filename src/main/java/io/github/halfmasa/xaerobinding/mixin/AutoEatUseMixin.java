package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import io.github.halfmasa.xaerobinding.feature.AutoEatService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class AutoEatUseMixin
{
    @Inject(method = "clearClientLevel", at = @At("TAIL"))
    private void halfmasa$clearFoodSession(Screen screen, CallbackInfo ci)
    {
        AutoEatService.getInstance().onWorldExit();
    }
    @WrapWithCondition(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"), require = 1)
    private boolean halfmasa$keepAutomaticFoodUse(MultiPlayerGameMode gameMode, Player player)
    {
        return !(player instanceof LocalPlayer local && AutoEatService.getInstance().ownsUse(local));
    }
}
//#endif
