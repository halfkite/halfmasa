package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import io.github.halfmasa.xaerobinding.compat.RenderPipelineCompatibility;

/** Read settings on the client thread; publish one immutable snapshot to mesh workers. */
public final class FluidTransparency implements IClientTickHandler {
    public static final FluidTransparency INSTANCE = new FluidTransparency();
    private static final long DEBOUNCE_NS = 200_000_000L;
    private static final Snapshot ORIGINAL = new Snapshot(10000, 10000);
    private static volatile Snapshot active = ORIGINAL;
    private Snapshot pending = ORIGINAL;
    private long changedAt;
    private Object previousLevel;

    private record Snapshot(int water, int lava) {}
    private FluidTransparency() {}

    private static Snapshot read() {
        if (!RenderPipelineCompatibility.supportsChunkRendering() || !Configs.FLUID_TRANSPARENCY.getBooleanValue()) return ORIGINAL;
        return new Snapshot(settingFactor(true), settingFactor(false));
    }

    private static int settingFactor(boolean water) {
        var enabled = water ? Configs.WATER_TRANSPARENCY_ENABLED : Configs.LAVA_TRANSPARENCY_ENABLED;
        var amount = water ? Configs.WATER_TRANSPARENCY : Configs.LAVA_TRANSPARENCY;
        // Stored as integer ten-thousandths to avoid floating point endpoint drift.
        return enabled.getBooleanValue()
                ? 10000 - Math.clamp(amount.getIntegerValue(), 0, 100) * (water ? 85 : 90) : 10000;
    }

    public static int insideFactor(FogType fluid) {
        if (!Configs.FLUID_TRANSPARENCY.getBooleanValue() || !Configs.FLUID_TRANSPARENCY_INSIDE.getBooleanValue()) {
            return 10000;
        }
        return switch (fluid) {
            case WATER -> settingFactor(true);
            case LAVA -> settingFactor(false);
            default -> 10000;
        };
    }

    @Override public void onClientTick(Minecraft client) {
        if (!Configs.isConfigLoaded()) return;
        Snapshot next = read();
        long now = System.nanoTime();
        if (!next.equals(pending)) { pending = next; changedAt = now; }
        if (client.level != previousLevel) {
            previousLevel = client.level;
            boolean changed = !next.equals(active);
            active = next;
            if (changed && client.level != null) MinecraftClientCompat.reloadLevelRenderer(client);
            return;
        }
        if (!active.equals(pending) && now - changedAt >= DEBOUNCE_NS) {
            active = pending;
            if (client.level != null) MinecraftClientCompat.reloadLevelRenderer(client);
        }
    }

    public static int factor(FluidState state) {
        Snapshot s = active;
        if (s.equals(ORIGINAL)) return 10000;
        if (state.is(FluidTags.WATER)) return s.water;
        if (state.is(FluidTags.LAVA)) return s.lava;
        return 10000;
    }

    /** ARGB or ABGR: alpha is the high byte in both formats. */
    public static int scaleAlpha(int color, int factor) {
        if (factor == 10000) return color;
        int alpha = color >>> 24;
        int scaled = (alpha * factor + 5000) / 10000;
        if (alpha != 0) scaled = Math.max(1, scaled);
        return (color & 0x00FFFFFF) | (scaled << 24);
    }
}
//#endif
