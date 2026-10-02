package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.InteractionResult;
import io.github.halfmasa.xaerobinding.feature.LitematicaMaterialRefill;

/** Observe schematic locals across old and new MaterialCache/PickBlock APIs. */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.util.EasyPlaceUtils", remap = false)
public abstract class LitematicaEasyPlaceRefillMixin
{
    @Unique private static BlockPos halfmasa$position;
    @Unique private static BlockState halfmasa$state;

    @Inject(method = "handleEasyPlace", at = @At("HEAD"))
    private static void halfmasa$resetTarget(CallbackInfoReturnable<InteractionResult> cir)
    {
        halfmasa$position = null;
        halfmasa$state = null;
    }

    @ModifyVariable(method = "handleEasyPlace", at = @At("STORE"), ordinal = 0)
    private static BlockPos halfmasa$capturePosition(BlockPos position)
    {
        halfmasa$position = position;
        return position;
    }

    @ModifyVariable(method = "handleEasyPlace", at = @At("STORE"), ordinal = 0)
    private static BlockState halfmasa$captureSchematicState(BlockState state)
    {
        halfmasa$state = state;
        if (halfmasa$position != null)
            LitematicaMaterialRefill.getInstance().onSchematicTarget(state, halfmasa$position);
        return state;
    }

    @ModifyVariable(method = "handleEasyPlace", at = @At("STORE"), ordinal = 0)
    private static ItemStack halfmasa$observeRequiredMaterial(ItemStack material)
    {
        if (halfmasa$position == null || halfmasa$state == null) return material;
        var refill = LitematicaMaterialRefill.getInstance();
        var client = Minecraft.getInstance();
        // Returning EMPTY prevents native picking/placement while waiting, and blocks a retry
        // at a different target before any inventory operation can run.
        if (refill.cancelTargetAttempt() || refill.afterPick(material, halfmasa$position,
                client, halfmasa$state, true)) return ItemStack.EMPTY;
        return material;
    }
}
