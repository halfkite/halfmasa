package io.github.halfmasa.xaerobinding.feature.minihud;

//#if MC >= 1.21.1 && MC < 1.21.11

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import fi.dy.masa.malilib.interfaces.IInventoryOverlayHandler;
import net.minecraft.client.gui.GuiGraphics;
import fi.dy.masa.malilib.render.InventoryOverlay.Context;
import fi.dy.masa.malilib.render.InventoryOverlayScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Keep content preparation separate from frame drawing; native target checks still run each frame. */
public final class LegacyHeldWorldPreview
{
    private static final Map<Object, State> STATES = new WeakHashMap<>();

    /** Implemented only on MaLiLib screens when MiniHUD compatibility is active. */
    public interface ScreenState
    {
        void halfmasa$setHeldContext(Context context);
    }

    private static final class State
    {
        Level world;
        BlockPos pos;
        BlockEntity blockEntity;
        BlockState blockState;
        CompoundTag data;
        long tick = Long.MIN_VALUE;
        long revision;
        Context context;
        InventoryOverlayScreen screen;
        boolean shulkerBackground;
        boolean villagerBackground;
    }

    private LegacyHeldWorldPreview() {}
    public static void clear() { STATES.clear(); }

    public static Context prepare(Object handler, Level world, BlockPos pos,
                                                  BlockEntity blockEntity, CompoundTag data,
                                                  Supplier<Context> original)
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
        Context context = original.get();
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

    public static void draw(IInventoryOverlayHandler handler, GuiGraphics ctx, Context context,
                            boolean shulkerBackground, boolean villagerBackground)
    {
        State state = STATES.computeIfAbsent(handler, ignored -> new State());
        if (state.screen == null || state.shulkerBackground != shulkerBackground ||
            state.villagerBackground != villagerBackground)
        {
            state.screen = new InventoryOverlayScreen(handler.getModId(), context, shulkerBackground, villagerBackground);
            state.screen.init(net.minecraft.client.Minecraft.getInstance(), 0, 0);
            state.shulkerBackground = shulkerBackground;
            state.villagerBackground = villagerBackground;
        }
        ((ScreenState) state.screen).halfmasa$setHeldContext(context);
        state.screen.render(ctx, 0, 0, 0);
    }
}
//#endif
