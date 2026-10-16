package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1 && MC < 1.21.11

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.render.InventoryOverlay.Context;
import fi.dy.masa.malilib.render.InventoryOverlay.Refresher;
import fi.dy.masa.malilib.render.InventoryOverlayScreen;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import io.github.halfmasa.xaerobinding.feature.minihud.LegacyHeldWorldPreview;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;

@Mixin(value = InventoryOverlayScreen.class, remap = false)
public abstract class MiniHudPreviewLegacyOverlayMixin implements LegacyHeldWorldPreview.ScreenState
{
    @Shadow String modId;
    @Shadow private Context previewData;
    @Unique private boolean halfmasa$held;
    @Unique private InventoryRenderType halfmasa$layoutType;
    @Unique private int halfmasa$layoutSlots;
    @Unique private InventoryOverlay.InventoryProperties halfmasa$layout;
    @Unique private long halfmasa$lastTick = Long.MIN_VALUE;
    @Unique private long halfmasa$lastRevision;
    @Unique private Level halfmasa$world;

    @Override public void halfmasa$setHeldContext(Context context)
    {
        this.previewData = context;
        this.halfmasa$held = true;
    }

    // Match the native invocation even when Screen.render is inherited and obfuscated.
    @WrapOperation(method = "*", at = @At(value = "INVOKE", desc = @Desc(owner = InventoryOverlay.class, value = "getInventoryPropsTemp", args = {InventoryRenderType.class, int.class}, ret = fi.dy.masa.malilib.render.InventoryOverlay.InventoryProperties.class)))
    private InventoryOverlay.InventoryProperties halfmasa$layout(InventoryRenderType type, int slots,
                                                                  Operation<InventoryOverlay.InventoryProperties> original)
    {
        if (!this.halfmasa$held || !PreviewCache.worldActive()) return original.call(type, slots);
        if (this.halfmasa$layout == null || this.halfmasa$layoutType != type || this.halfmasa$layoutSlots != slots)
        {
            var source = original.call(type, slots);
            // MaLiLib returns a shared mutable scratch object; retain an independent value instead.
            var copy = new InventoryOverlay.InventoryProperties();
            copy.totalSlots = source.totalSlots;
            copy.width = source.width;
            copy.height = source.height;
            copy.slotsPerRow = source.slotsPerRow;
            copy.slotOffsetX = source.slotOffsetX;
            copy.slotOffsetY = source.slotOffsetY;
            this.halfmasa$layout = copy;
            this.halfmasa$layoutType = type;
            this.halfmasa$layoutSlots = slots;
        }
        return this.halfmasa$layout;
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", desc = @Desc(owner = Refresher.class, value = "onContextRefresh", args = {Context.class, Level.class}, ret = Context.class)))
    private Context halfmasa$refresh(Refresher refresher, Context data, Level world, Operation<Context> original)
    {
        // Native held previews use a fresh screen each frame and never reach this screen refresher.
        // The held controller now refreshes content; reusing a screen must not add network requests.
        if (this.halfmasa$held && PreviewCache.worldActive()) return data;
        if (!"minihud".equals(this.modId) || !Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.getBooleanValue())
        {
            this.halfmasa$lastTick = Long.MIN_VALUE;
            return original.call(refresher, data, world);
        }
        long tick = PreviewCache.tick(), revision = PreviewCache.revision();
        if (this.halfmasa$world == world && this.halfmasa$lastTick == tick && this.halfmasa$lastRevision == revision) return data;
        this.halfmasa$world = world;
        this.halfmasa$lastTick = tick;
        this.halfmasa$lastRevision = revision;
        return original.call(refresher, data, world);
    }
}
//#endif
