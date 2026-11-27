package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.halfmasa.xaerobinding.feature.FluidTransparency;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
public abstract class FluidTransparencyWaterOverlayMixin {
    //#if MC >= 1.21.4
    //#if MC >= 26.3
    @ModifyExpressionValue(method = "lambda$submitWater$0", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/state/level/PlayerRenderState$WaterOverlay;color()I"))
    //#elseif MC >= 26.2
    //$$ @ModifyExpressionValue(method = "submitWater", at = @At(value = "INVOKE", target =
    //$$         "Lnet/minecraft/util/ARGB;colorFromFloat(FFFF)I"))
    //#else
    //$$ @ModifyExpressionValue(method = "renderWater", at = @At(value = "INVOKE", target =
    //$$         "Lnet/minecraft/util/ARGB;colorFromFloat(FFFF)I"))
    //#endif
    private static int halfmasa$reduceWaterOverlay(int color) {
        return FluidTransparency.scaleAlpha(color, FluidTransparency.insideFactor(FogType.WATER));
    }
    //#else
    //$$ @org.spongepowered.asm.mixin.injection.ModifyArg(method = "renderWater", at = @At(value = "INVOKE", ordinal = 0,
    //$$         target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V", remap = false), index = 3)
    //$$ private static float halfmasa$reduceWaterOverlay(float alpha) {
    //$$     return alpha * FluidTransparency.insideFactor(FogType.WATER) / 10000F;
    //$$ }
    //#endif
}
//#endif
