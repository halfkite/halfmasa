package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.feature.AutoEatService;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
//#if MC >= 26.0
import net.minecraft.world.inventory.ContainerInput;
//#else
//$$ import net.minecraft.world.inventory.ClickType;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class AutoEatInputMixin
{
    //#if MC >= 26.0
    @Inject(method = "handleContainerInput", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"))
    //#endif
    //#if MC >= 26.0
    private void halfmasa$manualFoodMenuInput(int menu, int slot, int button, ContainerInput input, Player player, CallbackInfo ci)
    //#else
    //$$ private void halfmasa$manualFoodMenuInput(int menu, int slot, int button, ClickType input, Player player, CallbackInfo ci)
    //#endif
    {
        AutoEatService.getInstance().onManualContainerInput();
    }
    @Inject(method = "attack", at = @At("HEAD"))
    private void halfmasa$pauseFoodForAttack(Player player, Entity target, CallbackInfo ci)
    {
        AutoEatService.getInstance().onAttack();
    }
}
//#endif
