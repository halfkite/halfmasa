package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1 && MC < 1.21.11

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.interfaces.IInventoryOverlayHandler;
import net.minecraft.client.gui.GuiGraphics;
import fi.dy.masa.malilib.render.InventoryOverlay.Context;
import net.minecraft.nbt.CompoundTag;
import io.github.halfmasa.xaerobinding.feature.minihud.LegacyHeldWorldPreview;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Desc(id = "getRenderContext", value = "getRenderContext", args = {GuiGraphics.class, ProfilerFiller.class, Minecraft.class}, ret = Context.class)
@Mixin(targets = "fi.dy.masa.minihud.renderer.InventoryOverlayHandler", remap = false)
public abstract class MiniHudPreviewLegacyWorldScopeMixin
{
    // Held world previews create a new screen each frame, so scope the actual handler entry.
    @WrapMethod(method = "@Desc(getRenderContext)")
    private Context halfmasa$worldPreviewScope(GuiGraphics ctx, ProfilerFiller profiler, Minecraft client,
                                                               Operation<Context> original)
    {
        PreviewCache.enterWorld();
        try { return original.call(ctx, profiler, client); }
        finally { PreviewCache.exitWorld(); }
    }

    @WrapMethod(method = "getTargetInventoryFromBlock")
    private Context halfmasa$prepareBlock(Level world, BlockPos pos, BlockEntity blockEntity,
                                                          CompoundTag data, Operation<Context> original)
    {
        return LegacyHeldWorldPreview.prepare(this, world, pos, blockEntity, data,
                () -> original.call(world, pos, blockEntity, data));
    }

//#if MC > 1.21.5
    @WrapOperation(method = "@Desc(getRenderContext)",
            at = @At(value = "INVOKE", desc = @Desc(value = "renderInventoryOverlay", args = {GuiGraphics.class, Context.class, Minecraft.class, boolean.class, boolean.class})))
//#else
//$$     @WrapOperation(method = "@Desc(getRenderContext)",
//$$             at = @At(value = "INVOKE", desc = @Desc(value = "renderInventoryOverlay", args = {Context.class, GuiGraphics.class, Minecraft.class, boolean.class, boolean.class})))
//#endif
//#if MC > 1.21.5
    private void halfmasa$drawPrepared(@Coerce IInventoryOverlayHandler handler, GuiGraphics ctx,
                                       Context context, Minecraft client, boolean shulkerBackground,
                                       boolean villagerBackground, Operation<Void> original)
//#else
//$$     private void halfmasa$drawPrepared(@Coerce IInventoryOverlayHandler handler, Context context,
//$$                                        GuiGraphics ctx, Minecraft client, boolean shulkerBackground,
//$$                                        boolean villagerBackground, Operation<Void> original)
//#endif
    {
        if (PreviewCache.worldActive()) LegacyHeldWorldPreview.draw(handler, ctx, context, shulkerBackground, villagerBackground);
//#if MC > 1.21.5
        else original.call(handler, ctx, context, client, shulkerBackground, villagerBackground);
//#else
//$$         else original.call(handler, context, ctx, client, shulkerBackground, villagerBackground);
//#endif
    }
}
//#endif
