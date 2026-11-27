package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
//#if MC >= 1.21.8
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.halfmasa.xaerobinding.feature.FluidTransparency;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
public abstract class FluidTransparencyFogMixin {
    //#if MC >= 1.21.11
    @WrapOperation(method = "setupFog", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/fog/environment/FogEnvironment;setupFog(Lnet/minecraft/client/renderer/fog/FogData;Lnet/minecraft/client/Camera;Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/DeltaTracker;)V"))
    private void halfmasa$reduceFluidFog(FogEnvironment environment, FogData fog, Camera camera, ClientLevel level,
                                        float renderDistance, DeltaTracker delta, Operation<Void> original) {
        original.call(environment, fog, camera, level, renderDistance, delta);
    //#else
    //$$ @WrapOperation(method = "setupFog", at = @At(value = "INVOKE", target =
    //$$         "Lnet/minecraft/client/renderer/fog/environment/FogEnvironment;setupFog(Lnet/minecraft/client/renderer/fog/FogData;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/DeltaTracker;)V"))
    //$$ private void halfmasa$reduceFluidFog(FogEnvironment environment, FogData fog,
    //$$         net.minecraft.world.entity.Entity entity, net.minecraft.core.BlockPos position, ClientLevel level,
    //$$         float renderDistance, DeltaTracker delta, Operation<Void> original) {
    //$$     original.call(environment, fog, entity, position, level, renderDistance, delta);
    //#endif
        FogType type;
        if (environment instanceof WaterFogEnvironment) type = FogType.WATER;
        else if (environment instanceof LavaFogEnvironment) type = FogType.LAVA;
        else return;

        int factor = FluidTransparency.insideFactor(type);
        if (factor == 10000) return;
        float opacity = factor / 10000.0F;
        fog.environmentalStart = fog.environmentalStart < 0
                ? fog.environmentalStart * opacity : fog.environmentalStart / opacity;
        fog.environmentalEnd /= opacity;
        fog.skyEnd /= opacity;
        fog.cloudEnd /= opacity;
        //#if MC >= 26.1
        fog.color.w *= opacity;
        //#endif
    }

    //#if MC < 26.1
    //$$ @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "computeFogColor", at = @At("RETURN"))
    //$$ private org.joml.Vector4f halfmasa$reduceFogColor(org.joml.Vector4f color, Camera camera) {
    //$$     int factor = FluidTransparency.insideFactor(camera.getFluidInCamera());
    //$$     if (factor == 10000) return color;
    //$$     return new org.joml.Vector4f(color.x, color.y, color.z, color.w * factor / 10000F);
    //$$ }
    //#endif
}
//#elseif MC >= 1.21.3
//$$ import com.llamalad7.mixinextras.injector.ModifyReturnValue;
//$$ import io.github.halfmasa.xaerobinding.feature.FluidTransparency;
//$$ import net.minecraft.client.Camera;
//$$ import net.minecraft.client.renderer.FogParameters;
//$$ import net.minecraft.client.renderer.FogRenderer;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$
//$$ @Mixin(FogRenderer.class)
//$$ public abstract class FluidTransparencyFogMixin {
//$$     @ModifyReturnValue(method = "setupFog", at = @At("RETURN"))
//$$     private static FogParameters halfmasa$reduceFluidFog(FogParameters fog, Camera camera) {
//$$         int factor = FluidTransparency.insideFactor(camera.getFluidInCamera());
//$$         if (factor == 10000 || fog == FogParameters.NO_FOG) return fog;
//$$         float opacity = factor / 10000F;
//$$         float start = fog.start() < 0 ? fog.start() * opacity : fog.start() / opacity;
//$$         return new FogParameters(start, fog.end() / opacity, fog.shape(),
//$$                 fog.red(), fog.green(), fog.blue(), fog.alpha() * opacity);
//$$     }
//$$ }
//#else
//$$ import io.github.halfmasa.xaerobinding.feature.FluidTransparency;
//$$ import net.minecraft.client.Camera;
//$$ import net.minecraft.client.renderer.FogRenderer;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.ModifyArg;
//$$
//$$ @Mixin(FogRenderer.class)
//$$ public abstract class FluidTransparencyFogMixin {
//$$     @ModifyArg(method = "setupFog", at = @At(value = "INVOKE",
//$$             target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogStart(F)V", remap = false))
//$$     private static float halfmasa$reduceFogStart(float start,
//$$             @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) Camera camera) {
//$$         float opacity = FluidTransparency.insideFactor(camera.getFluidInCamera()) / 10000F;
//$$         return start < 0 ? start * opacity : start / opacity;
//$$     }
//$$
//$$     @ModifyArg(method = "setupFog", at = @At(value = "INVOKE",
//$$             target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogEnd(F)V", remap = false))
//$$     private static float halfmasa$reduceFogEnd(float end,
//$$             @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) Camera camera) {
//$$         return end / (FluidTransparency.insideFactor(camera.getFluidInCamera()) / 10000F);
//$$     }
//$$ }
//#endif
//#endif
