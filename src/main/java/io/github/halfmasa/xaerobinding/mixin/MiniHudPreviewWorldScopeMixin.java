package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.3
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.interfaces.IInventoryOverlayHandler;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.InventoryOverlayContext;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import io.github.halfmasa.xaerobinding.feature.minihud.HeldWorldPreview;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "fi.dy.masa.minihud.renderer.InventoryOverlayHandler", remap = false)
public abstract class MiniHudPreviewWorldScopeMixin
{
    // Held world previews create a new screen each frame, so scope the actual handler entry.
    @WrapMethod(method = "getRenderContext(Lfi/dy/masa/malilib/render/GuiContext;Lnet/minecraft/util/profiling/ProfilerFiller;)Lfi/dy/masa/malilib/render/InventoryOverlayContext;")
    private InventoryOverlayContext halfmasa$worldPreviewScope(GuiContext ctx, ProfilerFiller profiler,
                                                               Operation<InventoryOverlayContext> original)
    {
        PreviewCache.enterWorld();
        try { return original.call(ctx, profiler); }
        finally { PreviewCache.exitWorld(); }
    }

    @WrapMethod(method = "getTargetInventoryFromBlock")
    private InventoryOverlayContext halfmasa$prepareBlock(Level world, BlockPos pos, BlockEntity blockEntity,
                                                          CompoundData data, Operation<InventoryOverlayContext> original)
    {
        return HeldWorldPreview.prepare(this, world, pos, blockEntity, data,
                () -> original.call(world, pos, blockEntity, data));
    }

    @WrapOperation(method = "getRenderContext(Lfi/dy/masa/malilib/render/GuiContext;Lnet/minecraft/util/profiling/ProfilerFiller;)Lfi/dy/masa/malilib/render/InventoryOverlayContext;",
            at = @At(value = "INVOKE", target = "Lfi/dy/masa/minihud/renderer/InventoryOverlayHandler;renderInventoryOverlay(Lfi/dy/masa/malilib/render/GuiContext;Lfi/dy/masa/malilib/render/InventoryOverlayContext;ZZ)V"))
    private void halfmasa$drawPrepared(@Coerce IInventoryOverlayHandler handler, GuiContext ctx,
                                       InventoryOverlayContext context, boolean shulkerBackground,
                                       boolean villagerBackground, Operation<Void> original)
    {
        if (PreviewCache.worldActive()) HeldWorldPreview.draw(handler, ctx, context, shulkerBackground, villagerBackground);
        else original.call(handler, ctx, context, shulkerBackground, villagerBackground);
    }
}
//#endif
