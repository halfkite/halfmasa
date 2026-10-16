package io.github.halfmasa.xaerobinding.feature.minihud;

//#if MC >= 26.3
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import fi.dy.masa.malilib.interfaces.IInventoryOverlayHandler;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.InventoryOverlayContext;
import fi.dy.masa.malilib.render.InventoryOverlayScreen;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Keep content preparation separate from frame drawing; native target checks still run each frame. */
public final class HeldWorldPreview
{
    private static final Map<Object, State> STATES = new WeakHashMap<>();

    /** Implemented only on MaLiLib screens when MiniHUD compatibility is active. */
    public interface ScreenState
    {
        void halfmasa$setHeldContext(InventoryOverlayContext context);
    }

    private static final class State
    {
        Level world;
        BlockPos pos;
        BlockEntity blockEntity;
        BlockState blockState;
        CompoundData data;
        long tick = Long.MIN_VALUE;
        long revision;
        InventoryOverlayContext context;
        InventoryOverlayScreen screen;
        boolean shulkerBackground;
        boolean villagerBackground;
    }

    private HeldWorldPreview() {}
    public static void clear() { STATES.clear(); }

    public static InventoryOverlayContext prepare(Object handler, Level world, BlockPos pos,
                                                  BlockEntity blockEntity, CompoundData data,
                                                  Supplier<InventoryOverlayContext> original)
    {
        if (!PreviewCache.worldActive()) return original.get();
        State state = STATES.computeIfAbsent(handler, ignored -> new State());
        long tick = PreviewCache.tick(), revision = PreviewCache.revision();
        BlockState blockState = world.getBlockState(pos);
        if (state.world == world && pos.equals(state.pos) && state.blockEntity == blockEntity &&
            state.blockState == blockState && state.data == data && state.tick == tick && state.revision == revision)
        {
            return state.context;
        }
        // Do not publish the key until preparation has completed successfully.
        InventoryOverlayContext context = original.get();
        state.world = world;
        state.pos = pos.immutable();
        state.blockEntity = blockEntity;
        state.blockState = blockState;
        state.data = data;
        state.tick = tick;
        state.revision = revision;
        state.context = context;
        return context;
    }

    public static void draw(IInventoryOverlayHandler handler, GuiContext ctx, InventoryOverlayContext context,
                            boolean shulkerBackground, boolean villagerBackground)
    {
        State state = STATES.computeIfAbsent(handler, ignored -> new State());
        if (state.screen == null || state.shulkerBackground != shulkerBackground ||
            state.villagerBackground != villagerBackground)
        {
            state.screen = new InventoryOverlayScreen(handler.getModId(), context, shulkerBackground, villagerBackground);
            state.screen.init(0, 0);
            state.shulkerBackground = shulkerBackground;
            state.villagerBackground = villagerBackground;
        }
        ((ScreenState) state.screen).halfmasa$setHeldContext(context);
        state.screen.extractRenderState(ctx, 0, 0, 0);
    }
}
//#endif
