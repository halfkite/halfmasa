package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.11
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.render.InventoryOverlayContext;
import fi.dy.masa.malilib.render.InventoryOverlayRefresher;
import fi.dy.masa.malilib.render.InventoryOverlayScreen;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.InventoryOverlayType;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import io.github.halfmasa.xaerobinding.feature.minihud.HeldWorldPreview;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;

@Mixin(value = InventoryOverlayScreen.class, remap = false)
public abstract class MiniHudPreviewOverlayMixin implements HeldWorldPreview.ScreenState
{
    @Shadow String modId;
    @Shadow private InventoryOverlayContext previewData;
    @Unique private boolean halfmasa$held;
    @Unique private InventoryOverlayType halfmasa$layoutType;
    @Unique private int halfmasa$layoutSlots;
    @Unique private InventoryOverlay.InventoryProperties halfmasa$layout;
    @Unique private long halfmasa$lastTick = Long.MIN_VALUE;
    @Unique private long halfmasa$lastRevision;
    @Unique private Level halfmasa$world;

    @Override public void halfmasa$setHeldContext(InventoryOverlayContext context)
    {
        this.previewData = context;
        this.halfmasa$held = true;
    }

    @WrapOperation(method = "renderData", at = @At(value = "INVOKE", desc = @Desc(owner = InventoryOverlay.class, value = "getInventoryPropsTemp", args = {InventoryOverlayType.class, int.class}, ret = fi.dy.masa.malilib.render.InventoryOverlay.InventoryProperties.class)))
    private InventoryOverlay.InventoryProperties halfmasa$layout(InventoryOverlayType type, int slots,
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

    @WrapOperation(method = "renderData", at = @At(value = "INVOKE", desc = @Desc(owner = InventoryOverlayRefresher.class, value = "onContextRefresh", args = {InventoryOverlayContext.class, Level.class}, ret = InventoryOverlayContext.class)))
    private InventoryOverlayContext halfmasa$refresh(InventoryOverlayRefresher refresher, InventoryOverlayContext data, Level world, Operation<InventoryOverlayContext> original)
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
